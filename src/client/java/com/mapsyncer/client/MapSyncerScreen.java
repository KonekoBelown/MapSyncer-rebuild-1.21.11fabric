package com.mapsyncer.client;

import com.mapsyncer.client.voxy.VoxySyncClient;
import com.mapsyncer.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MapSyncerScreen extends Screen {
    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 286;
    private static final int COMPLETE_VISIBLE_MS = 2000;
    private static final int ADMIN_POLL_MS = 2000;

    private Tab activeTab = Tab.SYNC;
    private Button syncCurrentButton;
    private Button syncAllButton;
    private Button radiusSyncButton;
    private Button radiusValueButton;
    private Button publicWaypointsButton;
    private Button voxyButton;
    private Button autoSyncButton;
    private Button hudButton;
    private Button delayValueButton;
    private Button chatIntervalValueButton;
    private Button incrementalButton;
    private Button forceButton;
    private Button radiusEnabledButton;
    private Button radiusModeButton;
    private Button radiusMaxValueButton;
    private Button radiusSaveButton;
    private EditBox dimensionBox;
    private int radiusSyncBlocks = 1000;
    private int adminRadiusMaxBlocks = 3000;
    private String adminRadiusMode = "PLAYER_POSITION";
    private long lastAdminPoll;

    public MapSyncerScreen() {
        super(Component.translatable("mapsyncer.gui.title"));
    }

    @Override
    protected void init() {
        if (activeTab == Tab.ADMIN && !isOwner()) {
            activeTab = Tab.SYNC;
        }
        rebuildGui();
    }

    @Override
    public void tick() {
        if (activeTab == Tab.ADMIN && !isOwner()) {
            activeTab = Tab.SYNC;
            rebuildGui();
            return;
        }
        if (activeTab == Tab.ADMIN && isOwner()) {
            long now = System.currentTimeMillis();
            if (now - lastAdminPoll > ADMIN_POLL_MS) {
                AdminStatusClientState.requestNow();
                lastAdminPoll = now;
            }
        }
        updateDynamicButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawShell(graphics);
        switch (activeTab) {
            case SYNC -> drawSyncTab(graphics);
            case ADMIN -> drawAdminTab(graphics);
            case SETTINGS -> drawSettingsTab(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildGui() {
        clearWidgets();
        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int x = contentLeft();
        int y = panelTop + 30;
        int gap = 8;
        int tabCount = isOwner() ? 3 : 2;
        int tabWidth = Math.min(108, Math.max(72, (contentWidth() - gap * (tabCount - 1)) / tabCount));

        addTabButton(Tab.SYNC, x, y, tabWidth);
        x += tabWidth + gap;
        if (isOwner()) {
            addTabButton(Tab.ADMIN, x, y, tabWidth);
            x += tabWidth + gap;
        }
        addTabButton(Tab.SETTINGS, x, y, tabWidth);

        switch (activeTab) {
            case SYNC -> buildSyncWidgets(panelLeft, panelTop);
            case ADMIN -> buildAdminWidgets(panelLeft, panelTop);
            case SETTINGS -> buildSettingsWidgets(panelLeft, panelTop);
        }
        if (activeTab == Tab.SYNC && MapPacketReceiver.isServerInstalled()) {
            VoxySyncClient.requestCapability();
        }
        updateDynamicButtons();
    }

    private void addTabButton(Tab tab, int x, int y, int width) {
        Button button = Button.builder(tab.title(), b -> {
            activeTab = tab;
            rebuildGui();
            if (activeTab == Tab.ADMIN) {
                AdminStatusClientState.requestNow();
                lastAdminPoll = System.currentTimeMillis();
            }
        }).bounds(x, y, width, 20).build();
        button.active = activeTab != tab;
        addRenderableWidget(button);
    }

    private void buildSyncWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = syncButtonY();
        int gap = 10;
        int buttonWidth = Math.max(1, (contentWidth() - gap) / 2);
        syncCurrentButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.sync.current"),
                b -> MapSyncerCommand.sendSyncRequest(Minecraft.getInstance(), currentDimensionId(), false)
        ).bounds(x, y, buttonWidth, 22).build());
        syncAllButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.sync.all"),
                b -> MapSyncerCommand.sendSyncRequest(Minecraft.getInstance(), "all", true)
        ).bounds(x + buttonWidth + gap, y, buttonWidth, 22).build());

        int radiusY = y + 28;
        int stepWidth = 32;
        int valueWidth = Math.min(96, Math.max(72, buttonWidth / 2));
        int radiusButtonWidth = contentWidth() - stepWidth * 2 - valueWidth - gap * 3;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            radiusSyncBlocks = Math.max(128, radiusSyncBlocks - 128);
            updateSettingsButtonMessages();
        }).bounds(x, radiusY, stepWidth, 22).build());
        radiusValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(x + stepWidth + gap, radiusY, valueWidth, 22).build());
        radiusValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            radiusSyncBlocks = Math.min(100_000, radiusSyncBlocks + 128);
            updateSettingsButtonMessages();
        }).bounds(x + stepWidth + gap + valueWidth + gap, radiusY, stepWidth, 22).build());
        radiusSyncButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.sync.radius"),
                b -> MapSyncerCommand.sendRadiusSyncRequest(Minecraft.getInstance(), radiusSyncBlocks)
        ).bounds(x + stepWidth * 2 + valueWidth + gap * 3, radiusY,
                Math.max(96, radiusButtonWidth), 22).build());
        publicWaypointsButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.waypoints.sync"),
                b -> PublicWaypointReceiver.requestManualSync()
        ).bounds(x, y + 56, contentWidth(), 22).build());
        voxyButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.voxy.sync_current"),
                b -> VoxySyncClient.startCurrentDimensionSync(Minecraft.getInstance())
        ).bounds(x, y + 84, contentWidth(), 22).build());
    }

    private void buildAdminWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = panelTop + 94;
        int contentWidth = contentWidth();
        int gap = 8;

        dimensionBox = addRenderableWidget(new EditBox(font, x, y, Math.min(238, contentWidth), 20,
                Component.translatable("mapsyncer.gui.admin.dimension")));
        dimensionBox.setMaxLength(128);
        dimensionBox.setValue(currentDimensionId());

        if (contentWidth >= 470) {
            int buttonWidth = Math.max(1, (contentWidth - gap * 2) / 3);
            incrementalButton = addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.incremental"),
                    b -> {
                        sendServerCommand("mapsyncer incremental run");
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x, y + 34, buttonWidth, 22).build());

            forceButton = addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.force"),
                    b -> {
                        String dimension = sanitizeDimensionInput(dimensionBox.getValue());
                        sendServerCommand("mapsyncer generate " + dimension + " force");
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x + buttonWidth + gap, y + 34, buttonWidth, 22).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("mapsyncer.gui.admin.refresh"),
                    b -> {
                        AdminStatusClientState.requestNow();
                        lastAdminPoll = System.currentTimeMillis();
                    }
            ).bounds(x + (buttonWidth + gap) * 2, y + 34, buttonWidth, 22).build());
        } else {
            int buttonWidth = Math.max(1, (contentWidth - gap) / 2);
            incrementalButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.incremental"),
                b -> {
                    sendServerCommand("mapsyncer incremental run");
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x, y + 34, buttonWidth, 22).build());

            forceButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.force"),
                b -> {
                    String dimension = sanitizeDimensionInput(dimensionBox.getValue());
                    sendServerCommand("mapsyncer generate " + dimension + " force");
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x + buttonWidth + gap, y + 34, buttonWidth, 22).build());

            addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.refresh"),
                b -> {
                    AdminStatusClientState.requestNow();
                    lastAdminPoll = System.currentTimeMillis();
                }
            ).bounds(x, y + 62, buttonWidth, 22).build());
        }

        int settingsY = y + (contentWidth >= 470 ? 68 : 92);
        radiusEnabledButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            PacketHandler.AdminStatusPayload status = AdminStatusClientState.getLastStatus();
            if (status != null) {
                AdminStatusClientState.setDraftRadiusEnabled(!status.radiusSyncEnabled());
            }
            updateDynamicButtons();
        }).bounds(x, settingsY, 94, 20).build());
        radiusModeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            adminRadiusMode = switch (adminRadiusMode) {
                case "PLAYER_POSITION" -> "WORLD_SPAWN";
                case "WORLD_SPAWN" -> "FIXED";
                default -> "PLAYER_POSITION";
            };
            AdminStatusClientState.setDraftRadiusMode(adminRadiusMode);
            updateDynamicButtons();
        }).bounds(x + 102, settingsY, 132, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            adminRadiusMaxBlocks = Math.max(128, adminRadiusMaxBlocks - 128);
            AdminStatusClientState.setDraftMaxRadius(adminRadiusMaxBlocks);
            updateDynamicButtons();
        }).bounds(x + 242, settingsY, 28, 20).build());
        radiusMaxValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(x + 274, settingsY, 74, 20).build());
        radiusMaxValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            adminRadiusMaxBlocks = Math.min(100_000, adminRadiusMaxBlocks + 128);
            AdminStatusClientState.setDraftMaxRadius(adminRadiusMaxBlocks);
            updateDynamicButtons();
        }).bounds(x + 352, settingsY, 28, 20).build());
        radiusSaveButton = addRenderableWidget(Button.builder(
                Component.translatable("mapsyncer.gui.admin.save_radius"),
                b -> AdminStatusClientState.sendSettingsUpdate()
        ).bounds(x + 388, settingsY, Math.max(96, contentWidth - 388), 20).build());

        AdminStatusClientState.requestNow();
        lastAdminPoll = System.currentTimeMillis();
    }

    private void buildSettingsWidgets(int panelLeft, int panelTop) {
        int x = contentLeft();
        int y = settingsStartY();
        int controlWidth = Math.min(130, Math.max(104, contentWidth() / 3));
        int controlX = contentRight() - controlWidth;
        int stepperWidth = Math.min(130, Math.max(122, controlWidth));
        int stepX = contentRight() - stepperWidth;
        int valueWidth = stepperWidth - 76;

        autoSyncButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            ClientConfig.VALUES.autoSyncOnJoin = !ClientConfig.VALUES.autoSyncOnJoin;
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(controlX, y, controlWidth, 20).build());

        hudButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            ClientConfig.VALUES.showSyncHud = !ClientConfig.VALUES.showSyncHud;
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(controlX, y + settingsRowGap(), controlWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            ClientConfig.VALUES.autoSyncDelaySeconds = Math.max(1, ClientConfig.VALUES.autoSyncDelaySeconds - 1);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX, y + settingsRowGap() * 2, 32, 20).build());
        delayValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(stepX + 38, y + settingsRowGap() * 2, valueWidth, 20).build());
        delayValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            ClientConfig.VALUES.autoSyncDelaySeconds = Math.min(60, ClientConfig.VALUES.autoSyncDelaySeconds + 1);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX + 44 + valueWidth, y + settingsRowGap() * 2, 32, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            ClientConfig.VALUES.syncProgressChatIntervalPercent = Math.max(0,
                    ClientConfig.VALUES.syncProgressChatIntervalPercent - 5);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX, y + settingsRowGap() * 3, 32, 20).build());
        chatIntervalValueButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
        }).bounds(stepX + 38, y + settingsRowGap() * 3, valueWidth, 20).build());
        chatIntervalValueButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            ClientConfig.VALUES.syncProgressChatIntervalPercent = Math.min(100,
                    ClientConfig.VALUES.syncProgressChatIntervalPercent + 5);
            ClientConfig.save();
            updateSettingsButtonMessages();
        }).bounds(stepX + 44 + valueWidth, y + settingsRowGap() * 3, 32, 20).build());

        updateSettingsButtonMessages();
    }

    private void drawShell(GuiGraphics graphics) {
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xD8181C22);
        graphics.renderOutline(left, top, panelWidth(), panelHeight(), 0xFF5BC0EB);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.fill(left + 12, top + 58, left + panelWidth() - 12, top + 59, 0x665BC0EB);
    }

    private void drawSyncTab(GuiGraphics graphics) {
        int x = contentLeft();
        int y = syncTextY();
        boolean installed = MapPacketReceiver.isServerInstalled();
        Component serverStatus = installed
                ? Component.translatable("mapsyncer.gui.sync.server_installed", MapPacketReceiver.getServerVersion())
                : Component.translatable("mapsyncer.gui.sync.server_missing");
        graphics.drawString(font, Component.translatable("mapsyncer.gui.sync.dimension", currentDimensionId()), x, y, 0xFFE8F0F6, false);
        graphics.drawString(font, serverStatus, x, y + 16, installed ? 0xFF8CE99A : 0xFFFFC857, false);

        int barY = syncBarY();
        int barWidth = contentWidth();
        int percent = visibleSyncPercent();
        graphics.drawString(font, Component.translatable("mapsyncer.gui.sync.progress",
                SyncProgressTracker.getProcessed(), SyncProgressTracker.getTotal(), percent), x, barY - 12, 0xFFE8F0F6, false);
        graphics.fill(x, barY, x + barWidth, barY + 8, 0xFF2E3740);
        graphics.fill(x, barY, x + (barWidth * percent) / 100, barY + 8, 0xFF5BC0EB);
        graphics.renderOutline(x, barY, barWidth, 8, 0xFF6B7785);
        if (!SyncProgressTracker.getStatus().isBlank()) {
            graphics.drawString(font, Component.literal(SyncProgressTracker.getStatus()), x, barY + 14, 0xFFB9C3CC, false);
        }

        int voxyY = syncVoxyY();
        PublicWaypointClientState.Status waypointStatus = PublicWaypointClientState.getStatus();
        Component waypointText = Component.translatable("mapsyncer.gui.waypoints.status",
                Component.translatable("mapsyncer.gui.waypoints.status." + waypointStatus.name().toLowerCase()).getString(),
                PublicWaypointClientState.getCount());
        drawSingleLine(graphics, waypointText, x, voxyY - 18, contentWidth(), 0xFFB9C3CC);

        String voxyReason = VoxySyncClient.getUnavailableReason(minecraft);
        Component voxyReasonText = switch (voxyReason) {
            case "" -> Component.translatable("mapsyncer.gui.voxy.reason.enabled");
            case "not_installed", "not_enabled", "server_disabled", "no_connection", "syncing",
                    "busy", "unknown", "region_dir_missing", "dimension_changed", "interrupted",
                    "failed", "completed", "client_io_failed", "import_busy", "import_failed",
                    "sending" -> Component.translatable("mapsyncer.gui.voxy.reason." + voxyReason);
            default -> Component.literal(voxyReason);
        };
        Component voxyStatus = Component.translatable("mapsyncer.gui.voxy.status", voxyReasonText.getString());
        drawSingleLine(graphics, voxyStatus, x, voxyY, contentWidth(), voxyReason.isBlank() ? 0xFF8CE99A : 0xFFB9C3CC);

        int voxyPercent = visibleVoxyPercent();
        graphics.drawString(font, Component.translatable("mapsyncer.gui.voxy.progress",
                VoxySyncClient.getProcessedRegions(), VoxySyncClient.getTotalRegions(), voxyPercent),
                x, voxyY + 14, 0xFFE8F0F6, false);
        graphics.fill(x, voxyY + 28, x + barWidth, voxyY + 36, 0xFF2E3740);
        graphics.fill(x, voxyY + 28, x + (barWidth * voxyPercent) / 100, voxyY + 36, 0xFFB089FF);
        graphics.renderOutline(x, voxyY + 28, barWidth, 8, 0xFF6B7785);

        String voxyText = VoxySyncClient.getDisplayStatus();
        if (!voxyText.isBlank() && voxyY + 52 <= panelTop() + panelHeight() - 4) {
            drawSingleLine(graphics, Component.literal(voxyText), x, voxyY + 42, contentWidth(), 0xFFB9C3CC);
        }

        int noteY = voxyY + 58;
        if (panelHeight() >= 270 && noteY + 18 <= panelTop() + panelHeight() - 8) {
            graphics.drawWordWrap(font, Component.translatable("mapsyncer.gui.voxy.note"),
                    x, noteY, contentWidth(), 0xFF9BA8B5);
        }
    }

    private void drawAdminTab(GuiGraphics graphics) {
        int x = contentLeft();
        int y = panelTop() + 72;
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.dimension_label"), x, y + 10, 0xFFE8F0F6, false);
        graphics.drawWordWrap(font, Component.translatable("mapsyncer.gui.admin.warning"), x, y + 90, contentWidth(), 0xFFFFC857);

        int statusY = y + (contentWidth() >= 470 ? 124 : 134);
        String error = AdminStatusClientState.getLastError();
        PacketHandler.AdminStatusPayload status = AdminStatusClientState.getLastStatus();
        if (!error.isBlank()) {
            graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.status_" + error), x, statusY, 0xFFFF6B6B, false);
            return;
        }
        if (status == null) {
            graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.status_waiting"), x, statusY, 0xFFB9C3CC, false);
            return;
        }
        if (!status.allowed()) {
            graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.no_permission"), x, statusY, 0xFFFF6B6B, false);
            return;
        }

        int percent = status.total() > 0 ? Math.min(100, status.processed() * 100 / status.total()) : 0;
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.status_line",
                status.running() ? Component.translatable("mapsyncer.gui.admin.running") : Component.translatable("mapsyncer.gui.admin.idle"),
                status.processed(), status.total(), percent, status.status()), x, statusY, 0xFFE8F0F6, false);
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.dirty", status.dirtyCount()), x, statusY + 16, 0xFFB9C3CC, false);
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.cache",
                status.cacheDimensionCount(), status.cacheRegionCount(), status.cacheSizeBytes() / (1024.0 * 1024.0)), x, statusY + 32, 0xFFB9C3CC, false);
        String speedLimit = status.syncSpeedLimitKBps() <= 0
                ? Component.translatable("mapsyncer.gui.admin.unlimited").getString()
                : status.syncSpeedLimitKBps() + " KB/s";
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.limit", speedLimit), x, statusY + 48, 0xFFB9C3CC, false);
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.incremental_status", status.incrementalStatus()), x, statusY + 64, 0xFFB9C3CC, false);
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.radius_status",
                status.radiusSyncEnabled() ? Component.translatable("mapsyncer.gui.settings.on").getString()
                        : Component.translatable("mapsyncer.gui.settings.off").getString(),
                status.maxRadiusSyncBlocks(), status.radiusSyncCenterMode()), x, statusY + 80, 0xFFB9C3CC, false);
        String waypointHash = status.publicWaypointsHash().isBlank()
                ? "-"
                : status.publicWaypointsHash().substring(0, Math.min(8, status.publicWaypointsHash().length()));
        graphics.drawString(font, Component.translatable("mapsyncer.gui.admin.waypoints",
                status.publicWaypointsEnabled() ? Component.translatable("mapsyncer.gui.settings.on").getString()
                        : Component.translatable("mapsyncer.gui.settings.off").getString(),
                status.publicWaypointsGroup(), status.publicWaypointsCount(), waypointHash),
                x, statusY + 96, 0xFFB9C3CC, false);
    }

    private void drawSettingsTab(GuiGraphics graphics) {
        int x = contentLeft();
        int y = settingsStartY();
        int textWidth = Math.max(118, contentRight() - x - Math.min(130, Math.max(104, contentWidth() / 3)) - 12);
        int rowGap = settingsRowGap();
        drawSetting(graphics, "mapsyncer.gui.settings.auto_sync", "mapsyncer.gui.settings.auto_sync.desc", x, y, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.hud", "mapsyncer.gui.settings.hud.desc", x, y + rowGap, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.delay", "mapsyncer.gui.settings.delay.desc", x, y + rowGap * 2, textWidth);
        drawSetting(graphics, "mapsyncer.gui.settings.chat", "mapsyncer.gui.settings.chat.desc", x, y + rowGap * 3, textWidth);
        int noteY = y + rowGap * 4 + 4;
        if (noteY + 24 < panelTop() + panelHeight()) {
            graphics.drawWordWrap(font, Component.translatable("mapsyncer.gui.settings.server_limit_note"),
                    x, noteY, contentWidth(), 0xFFB9C3CC);
        }
    }

    private void drawSetting(GuiGraphics graphics, String titleKey, String descKey, int x, int y, int textWidth) {
        graphics.drawString(font, Component.translatable(titleKey), x, y + 2, 0xFFE8F0F6, false);
        graphics.drawWordWrap(font, Component.translatable(descKey), x, y + 16, textWidth, 0xFF9BA8B5);
    }

    private void drawSingleLine(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        String ellipsis = "...";
        int limit = Math.max(1, maxWidth - font.width(ellipsis));
        String trimmed = font.plainSubstrByWidth(value, limit) + ellipsis;
        graphics.drawString(font, Component.literal(trimmed), x, y, color, false);
    }

    private void updateDynamicButtons() {
        boolean canSync = minecraft != null && minecraft.player != null && minecraft.level != null
                && MapPacketReceiver.isServerInstalled() && !MapPacketReceiver.isSyncInProgress();
        if (syncCurrentButton != null) {
            syncCurrentButton.active = canSync;
        }
        if (syncAllButton != null) {
            syncAllButton.active = canSync;
        }
        if (radiusSyncButton != null) {
            radiusSyncButton.active = canSync && ClientPlayNetworking.canSend(PacketHandler.RadiusSyncRequestPayload.TYPE);
        }
        if (publicWaypointsButton != null) {
            publicWaypointsButton.active = minecraft != null && minecraft.player != null
                    && ClientPlayNetworking.canSend(PacketHandler.PublicWaypointsRequestPayload.TYPE);
        }
        if (voxyButton != null) {
            if (MapPacketReceiver.isServerInstalled()) {
                VoxySyncClient.requestCapability();
            }
            voxyButton.active = VoxySyncClient.canStart(minecraft);
        }
        boolean canAdminAction = isOwner() && minecraft != null && minecraft.player != null;
        if (incrementalButton != null) {
            incrementalButton.active = canAdminAction;
        }
        if (forceButton != null) {
            forceButton.active = canAdminAction && dimensionBox != null && !sanitizeDimensionInput(dimensionBox.getValue()).isBlank();
        }
        if (radiusSaveButton != null) {
            radiusSaveButton.active = canAdminAction && ClientPlayNetworking.canSend(PacketHandler.AdminSettingsUpdatePayload.TYPE);
        }
        updateSettingsButtonMessages();
    }

    private void updateSettingsButtonMessages() {
        if (autoSyncButton != null) {
            autoSyncButton.setMessage(toggleLabel(ClientConfig.VALUES.autoSyncOnJoin));
        }
        if (hudButton != null) {
            hudButton.setMessage(toggleLabel(ClientConfig.VALUES.showSyncHud));
        }
        if (delayValueButton != null) {
            delayValueButton.setMessage(Component.translatable("mapsyncer.gui.settings.seconds",
                    ClientConfig.VALUES.autoSyncDelaySeconds));
        }
        if (chatIntervalValueButton != null) {
            chatIntervalValueButton.setMessage(Component.translatable("mapsyncer.gui.settings.percent",
                    ClientConfig.VALUES.syncProgressChatIntervalPercent));
        }
        if (radiusValueButton != null) {
            radiusValueButton.setMessage(Component.translatable("mapsyncer.gui.sync.radius_value", radiusSyncBlocks));
        }
        PacketHandler.AdminStatusPayload status = AdminStatusClientState.getLastStatus();
        if (status != null && !AdminStatusClientState.hasDraft()) {
            adminRadiusMaxBlocks = status.maxRadiusSyncBlocks();
            adminRadiusMode = status.radiusSyncCenterMode();
        } else {
            adminRadiusMaxBlocks = AdminStatusClientState.getDraftMaxRadius(adminRadiusMaxBlocks);
            adminRadiusMode = AdminStatusClientState.getDraftRadiusMode(adminRadiusMode);
        }
        if (radiusEnabledButton != null) {
            boolean enabled = AdminStatusClientState.getDraftRadiusEnabled(status != null && status.radiusSyncEnabled());
            radiusEnabledButton.setMessage(toggleLabel(enabled));
        }
        if (radiusModeButton != null) {
            radiusModeButton.setMessage(Component.translatable("mapsyncer.gui.admin.radius_mode." + adminRadiusMode));
        }
        if (radiusMaxValueButton != null) {
            radiusMaxValueButton.setMessage(Component.literal(String.valueOf(adminRadiusMaxBlocks)));
        }
    }

    private Component toggleLabel(boolean enabled) {
        return Component.translatable(enabled ? "mapsyncer.gui.settings.on" : "mapsyncer.gui.settings.off");
    }

    private int visibleSyncPercent() {
        if (SyncProgressTracker.isTracking()) {
            return SyncProgressTracker.getPercent();
        }
        long completedAt = SyncProgressTracker.getCompletedAt();
        if (completedAt > 0 && System.currentTimeMillis() - completedAt <= COMPLETE_VISIBLE_MS) {
            return 100;
        }
        return 0;
    }

    private int visibleVoxyPercent() {
        if (VoxySyncClient.isSyncing()) {
            return VoxySyncClient.getPercent();
        }
        long completedAt = VoxySyncClient.getCompletedAt();
        if (completedAt > 0 && System.currentTimeMillis() - completedAt <= COMPLETE_VISIBLE_MS) {
            return 100;
        }
        return 0;
    }

    private boolean isOwner() {
        return minecraft != null && minecraft.player != null
                && Commands.LEVEL_OWNERS.check(minecraft.player.permissions());
    }

    private String currentDimensionId() {
        return MapSyncerCommand.currentDimensionId(Minecraft.getInstance());
    }

    private String sanitizeDimensionInput(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.isBlank() ? currentDimensionId() : value.replace(" ", "");
    }

    private void sendServerCommand(String command) {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
        }
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(1, width - 32));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(1, height - 32));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(16, (height - panelHeight()) / 2);
    }

    private int panelMargin() {
        int target = panelWidth() < 430 ? 20 : 28;
        return Math.max(1, Math.min(target, panelWidth() / 8));
    }

    private int contentLeft() {
        return panelLeft() + panelMargin();
    }

    private int contentRight() {
        return panelLeft() + panelWidth() - panelMargin();
    }

    private int contentWidth() {
        return contentRight() - contentLeft();
    }

    private int settingsStartY() {
        return panelTop() + (panelHeight() < 250 ? 72 : 88);
    }

    private int settingsRowGap() {
        return panelHeight() < 250 ? 40 : 48;
    }

    private int syncTextY() {
        return panelTop() + (panelHeight() < 270 ? 66 : 72);
    }

    private int syncButtonY() {
        return panelTop() + (panelHeight() < 270 ? 94 : 104);
    }

    private int syncBarY() {
        return syncButtonY() + (panelHeight() < 270 ? 110 : 116);
    }

    private int syncVoxyY() {
        return syncBarY() + (panelHeight() < 270 ? 34 : 32);
    }

    private enum Tab {
        SYNC("mapsyncer.gui.tab.sync"),
        ADMIN("mapsyncer.gui.tab.admin"),
        SETTINGS("mapsyncer.gui.tab.settings");

        private final String key;

        Tab(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(key);
        }
    }
}
