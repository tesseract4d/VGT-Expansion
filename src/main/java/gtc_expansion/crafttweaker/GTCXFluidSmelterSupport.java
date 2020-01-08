package gtc_expansion.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import gtc_expansion.recipes.GTCXRecipeLists;
import gtc_expansion.tile.GTCXTileFluidSmelter;
import gtclassic.api.crafttweaker.GTCraftTweakerActions;
import ic2.api.recipe.IRecipeInput;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Locale;

@ZenClass("mods.gtclassic.FluidSmelter")
@ZenRegister
public class GTCXFluidSmelterSupport {
    @ZenMethod
    public static void addRecipe( ILiquidStack output, IIngredient input1, int requiredHeat, @Optional(valueLong = 2560L)int totalEu){
        GTCraftTweakerActions.apply(new FluidSmelterRecipeAction(GTCraftTweakerActions.of(input1), requiredHeat, totalEu, CraftTweakerMC.getLiquidStack(output)));
    }

    @ZenMethod
    public static void addCoil(IItemStack stack, int heatValue){
        GTCraftTweakerActions.apply(new FluidSmelterCoilAction(CraftTweakerMC.getItemStack(stack), heatValue));
    }

    private static final class FluidSmelterCoilAction implements IAction {
        private final ItemStack stack;
        private final int heatValue;

        FluidSmelterCoilAction(ItemStack stack, int heatValue) {
            this.stack = stack;
            this.heatValue = heatValue;
        }

        @Override
        public void apply() {
            if (heatValue <= 0) {
                CraftTweakerAPI.logError(CraftTweakerAPI.getScriptFileAndLine() + " > "
                        + "Heat value must be greater then 0!!");
                return;
            }
            GTCXTileFluidSmelter.coilsSlotWhitelist.put(stack, heatValue);
        }

        @Override
        public String describe() {
            return String.format(Locale.ENGLISH, "Add Coil[%s for %s heat] to %s", stack, heatValue, GTCXTileFluidSmelter.coilsSlotWhitelist);
        }
    }

    private static final class FluidSmelterRecipeAction implements IAction {

        private final IRecipeInput input1;
        private final int requiredHeat;
        private final int totalEu;
        private final FluidStack output;

        FluidSmelterRecipeAction(IRecipeInput input1, int requiredHeat, int totalEu, FluidStack output) {
            this.input1 = input1;
            this.requiredHeat = requiredHeat;
            this.totalEu = totalEu;
            this.output = output;
        }

        @Override
        public void apply() {
            if (totalEu <= 0) {
                CraftTweakerAPI.logError(CraftTweakerAPI.getScriptFileAndLine() + " > "
                        + "Eu amount must be greater then 0!!");
                return;
            }
            if (requiredHeat <= 0) {
                CraftTweakerAPI.logError(CraftTweakerAPI.getScriptFileAndLine() + " > "
                        + "required heat amount must be greater then 0!!");
                return;
            }
            GTCXTileFluidSmelter.addRecipe(input1, requiredHeat, totalEu, output);
        }

        @Override
        public String describe() {
            return String.format(Locale.ENGLISH, "Add Recipe[%s, %s, %s, %s -> %s] to %s", input1, requiredHeat, totalEu, output, GTCXRecipeLists.FLUID_SMELTER_RECIPE_LIST);
        }
    }
}
