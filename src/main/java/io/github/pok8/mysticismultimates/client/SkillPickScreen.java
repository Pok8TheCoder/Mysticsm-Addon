package io.github.pok8.mysticismultimates.client;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.client.screen.templates.IScrollBar;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class SkillPickScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<SkillPickMenu> implements IScrollBar {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("tensura", "textures/gui/skill_creation/skill_creation.png");
    private static final ResourceLocation ABILITY_BAR = ResourceLocation.fromNamespaceAndPath("tensura", "textures/gui/ability_button.png");

    private float scrollOffset;
    private boolean scrolling;
    private int listStartIndex;
    private int textStartIndex;
    private String nameFilter = "";
    private EditBox searchField;
    private List<ManasSkill> filtered = List.of();
    private ManasSkill selectedSkill;

    public SkillPickScreen(SkillPickMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 233;
        this.imageHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0.0f;
        listStartIndex = 0;
        searchField = new EditBox(font, leftPos + 19, topPos + 27, 79, 9, Component.empty());
        searchField.setBordered(false);
        searchField.setResponder(value -> {
            nameFilter = value == null ? "" : value;
            scrollOffset = 0.0f;
            listStartIndex = 0;
            updateFiltered();
        });
        addRenderableWidget(searchField);
        updateFiltered();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selectedSkill != null && mouseX >= leftPos + 156 && mouseX < leftPos + 188 && mouseY >= topPos + 6 && mouseY < topPos + 38) {
            graphics.renderTooltip(font, selectedSkill.getName(), mouseX, mouseY);
        } else if (selectedSkill != null && mouseX >= leftPos + 162 && mouseX < leftPos + 182 && mouseY >= topPos + 116 && mouseY < topPos + 136) {
            graphics.renderTooltip(font, Component.translatable("mysticism_ultimates.skill.pick.confirm"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        graphics.drawString(font, title, 56 - titleWidth / 2, titleLabelY, 0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int shown = Math.min(filtered.size(), listStartIndex + getScrollBarRenderCount());
        for (int index = listStartIndex; index < shown; index++) {
            int x = leftPos + 6;
            int y = topPos + 43 + (index - listStartIndex) * 13;
            ManasSkill skill = filtered.get(index);
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + 89 && mouseY < y + 13;
            int v = hovered || skill == selectedSkill ? 13 : 0;
            graphics.blit(ABILITY_BAR, x, y, 0.0f, v, 89, 13, 89, 26);
            Component name = skill instanceof TensuraSkill tensuraSkill ? tensuraSkill.getColoredName() : skill.getName();
            graphics.drawString(font, name, x + 3, y + 3, 0xFFFFFF, false);
        }
        renderScrollBar(graphics, mouseX, mouseY);
        if (selectedSkill == null) {
            return;
        }
        ResourceLocation icon = selectedSkill.getSkillIcon();
        if (icon != null) {
            graphics.blit(icon, leftPos + 156, topPos + 6, 0.0f, 0.0f, 32, 32, 32, 32);
        }
        List<FormattedCharSequence> lines = font.split(FormattedText.of(selectedSkill.getSkillDescription().getString()), 94);
        int start = Math.min(textStartIndex, Math.max(0, lines.size() - 6));
        int end = Math.min(lines.size(), start + 6);
        for (int line = start; line < end; line++) {
            graphics.drawString(font, lines.get(line), leftPos + 125, topPos + 48 + (line - start) * 9, 0x858585, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickedScrollBar(mouseX, mouseY)) {
            return true;
        }
        if (searchField != null && !searchField.isHovered()) {
            searchField.setFocused(false);
        }
        if (selectedSkill != null && mouseX >= leftPos + 162 && mouseX < leftPos + 182 && mouseY >= topPos + 116 && mouseY < topPos + 136) {
            int index = menu.getSkills().indexOf(selectedSkill);
            if (index >= 0 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                return true;
            }
        }
        int shown = Math.min(filtered.size(), listStartIndex + getScrollBarRenderCount());
        for (int index = listStartIndex; index < shown; index++) {
            int x = leftPos + 6;
            int y = topPos + 43 + (index - listStartIndex) * 13;
            if (mouseX >= x && mouseY >= y && mouseX < x + 89 && mouseY < y + 13) {
                selectedSkill = filtered.get(index);
                textStartIndex = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && draggedScrollBar(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + 125 && mouseX < leftPos + 219 && mouseY >= topPos + 43 && mouseY < topPos + 109) {
            textStartIndex = Math.max(0, textStartIndex + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return scrolledScrollBar(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (searchField != null && searchField.isFocused() && keyCode != 256) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateFiltered() {
        String filter = nameFilter == null ? "" : nameFilter.trim().toLowerCase();
        if (filter.isEmpty()) {
            filtered = new ArrayList<>(menu.getSkills());
            return;
        }
        filtered = new ArrayList<>();
        for (ManasSkill skill : menu.getSkills()) {
            if (skill.getName().getString().toLowerCase().contains(filter)) {
                filtered.add(skill);
            }
        }
    }

    @Override
    public int getScrollBarX() {
        return leftPos + 98;
    }

    @Override
    public int getScrollBarY() {
        return topPos + 43;
    }

    @Override
    public int getScrollBarTotalSpace() {
        return 91;
    }

    @Override
    public int getScrollBarListSize() {
        return filtered.size();
    }

    @Override
    public int getScrollBarRenderCount() {
        return 7;
    }

    @Override
    public float getScrollOffset() {
        return scrollOffset;
    }

    @Override
    public void setScrollOffset(float scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    @Override
    public boolean isScrolling() {
        return scrolling;
    }

    @Override
    public void setScrolling(boolean scrolling) {
        this.scrolling = scrolling;
    }

    public int getListStartIndex() {
        return listStartIndex;
    }

    @Override
    public void setListStartIndex(int listStartIndex) {
        this.listStartIndex = listStartIndex;
    }
}
