package gtc_expansion.tile.steam;

import gtc_expansion.GTCExpansion;
import gtc_expansion.container.GTCXContainerSteamExtractor;
import gtc_expansion.container.GTCXContainerSteamMacerator;
import gtc_expansion.tile.GTCXTileStoneExtractor;
import gtc_expansion.tile.base.GTCXTileBaseSteamMachine;
import gtc_expansion.util.GTCXSteamMachineFilter;
import gtclassic.api.recipe.GTRecipeMultiInputList;
import ic2.core.inventory.container.ContainerIC2;
import ic2.core.inventory.filters.IFilter;
import ic2.core.inventory.gui.custom.MachineGui;
import ic2.core.platform.registry.Ic2Sounds;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GTCXTileSteamMacerator extends GTCXTileBaseSteamMachine {
    public static final ResourceLocation GUI_LOCATION = new ResourceLocation(GTCExpansion.MODID, "textures/gui/bronzemacerator.png");
    public IFilter filter = new GTCXSteamMachineFilter(this);
    public GTCXTileSteamMacerator() {
        super(2, 800, 4);
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{0};
    }

    @Override
    public IFilter[] getInputFilters(int[] slots) {
        return new IFilter[]{filter};
    }

    @Override
    public boolean isRecipeSlot(int slot) {
        return slot != 1;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{1};
    }

    @Override
    public GTRecipeMultiInputList getRecipeList() {
        return GTCXTileStoneExtractor.RECIPE_LIST;
    }

    @Override
    public ResourceLocation getStartSoundFile() {
        return Ic2Sounds.maceratorOp;
    }

    @Override
    public ContainerIC2 getGuiContainer(EntityPlayer entityPlayer) {
        return new GTCXContainerSteamMacerator(entityPlayer.inventory, this);
    }

    @Override
    public Class<? extends GuiScreen> getGuiClass(EntityPlayer entityPlayer) {
        return MachineGui.MaceratorGui.class;
    }

    public ResourceLocation getGuiLocation(){
        return GUI_LOCATION;
    }
}
