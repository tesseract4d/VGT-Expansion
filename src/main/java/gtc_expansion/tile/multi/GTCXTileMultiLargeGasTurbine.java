package gtc_expansion.tile.multi;

import gtc_expansion.data.GTCXBlocks;
import gtc_expansion.data.GTCXItems;
import gtc_expansion.container.GTCXContainerLargeGasTurbine;
import gtc_expansion.interfaces.IGTMultiTileProduction;
import gtc_expansion.interfaces.IGTOwnerTile;
import gtc_expansion.recipes.GTCXRecipeLists;
import gtc_expansion.tile.GTCXTileCasing;
import gtc_expansion.tile.hatch.GTCXTileEnergyOutputHatch.GTCXTileDynamoHatch;
import gtc_expansion.tile.hatch.GTCXTileItemFluidHatches.GTCXTileInputHatch;
import gtclassic.api.helpers.int3;
import gtclassic.api.interfaces.IGTMultiTileStatus;
import gtclassic.api.recipe.GTRecipeMultiInputList;
import gtclassic.api.recipe.GTRecipeMultiInputList.MultiRecipe;
import ic2.api.classic.recipe.crafting.RecipeInputFluid;
import ic2.api.classic.recipe.machine.MachineOutput;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.network.INetworkTileEntityEventListener;
import ic2.api.recipe.IRecipeInput;
import ic2.core.block.base.tile.TileEntityMachine;
import ic2.core.inventory.base.IHasGui;
import ic2.core.inventory.container.ContainerIC2;
import ic2.core.inventory.gui.GuiComponentContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Predicate;

public class GTCXTileMultiLargeGasTurbine extends TileEntityMachine implements ITickable, IHasGui, IGTMultiTileStatus, IGTMultiTileProduction, IGTOwnerTile, INetworkClientTileEntityEventListener, INetworkTileEntityEventListener {
    public boolean lastState;
    public boolean firstCheck = true;
    private BlockPos input1;
    private BlockPos input2;
    private BlockPos dynamo;
    int production;
    protected MultiRecipe lastRecipe;
    int ticker = 0;
    protected boolean shouldCheckRecipe;
    public static final String RECIPE_TICKS = "recipeTicks";
    public static final String RECIPE_EU = "recipeEu";
    public static final IBlockState reinforcedCasingState = GTCXBlocks.casingReinforced.getDefaultState();
    public static final IBlockState inputHatchState = GTCXBlocks.inputHatch.getDefaultState();
    public static final IBlockState dynamoHatchState = GTCXBlocks.dynamoHatch.getDefaultState();

    public GTCXTileMultiLargeGasTurbine() {
        super(1);
        this.addGuiFields("lastState", "production");
        shouldCheckRecipe = true;
        input1 = this.getPos();
        input2 = this.getPos();
        dynamo = this.getPos();
        production = 0;
    }


    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.lastState = nbt.getBoolean("lastState");
        this.ticker = nbt.getInteger("ticker");
        this.input1 = readBlockPosFromNBT(nbt, "input1");
        this.input2 = readBlockPosFromNBT(nbt, "input2");
        this.dynamo = readBlockPosFromNBT(nbt, "dynamo");
        this.production = nbt.getInteger("production");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("lastState", this.lastState);
        nbt.setInteger("ticker", ticker);
        writeBlockPosToNBT(nbt, "input1", input1);
        writeBlockPosToNBT(nbt, "input2", input2);
        writeBlockPosToNBT(nbt, "dynamo", dynamo);
        nbt.setInteger("production", production);
        return nbt;
    }

    public void onBlockRemoved() {
        removeRing(new int3(getPos(), getFacing()));
    }

    public void onBlockPlaced(){
        getNetwork().initiateClientTileEntityEvent(this, 1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        shouldCheckRecipe = true;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        super.setStackInSlot(slot, stack);
        shouldCheckRecipe = true;
    }

    public void writeBlockPosToNBT(NBTTagCompound nbt, String id, BlockPos pos){
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("X", pos.getX());
        compound.setInteger("Y", pos.getY());
        compound.setInteger("Z", pos.getZ());
        nbt.setTag(id, compound);
    }

    public BlockPos readBlockPosFromNBT(NBTTagCompound nbt, String id){
        NBTTagCompound compound = nbt.getCompoundTag(id);
        int x = compound.getInteger("X");
        int y = compound.getInteger("Y");
        int z = compound.getInteger("Z");
        return new BlockPos(x, y, z);
    }

    public boolean canWork() {
        if (this.world.getTotalWorldTime() % 256L == 0L || this.firstCheck) {
            this.lastState = this.checkStructure();
            if (this.firstCheck){
                this.firstCheck = false;
            }
            this.getNetwork().updateTileGuiField(this, "lastState");
        }
        return this.lastState;
    }

    public boolean isTurbineRotor(ItemStack stack){
        return stack.getItem() == GTCXItems.bronzeTurbineRotor || stack.getItem() == GTCXItems.steelTurbineRotor || stack.getItem() == GTCXItems.magnaliumTurbineRotor || stack.getItem() == GTCXItems.tungstensteelTurbineRotor || stack.getItem() == GTCXItems.carbonTurbineRotor;
    }


    @Override
    public void update() {
        if (ticker < 80){
            ticker++;
        }
        boolean canWork = canWork() && world.getTileEntity(input1) instanceof GTCXTileInputHatch && world.getTileEntity(dynamo) instanceof GTCXTileDynamoHatch;
        if (canWork && isTurbineRotor(this.getStackInSlot(0))){
            if (this.shouldCheckRecipe) {
                this.lastRecipe = this.getRecipe();
                this.shouldCheckRecipe = false;
            }
            boolean operate = this.lastRecipe != null && this.lastRecipe != GTRecipeMultiInputList.INVALID_RECIPE;
            if (operate){
                GTCXTileInputHatch inputHatch = (GTCXTileInputHatch) world.getTileEntity(input1);
                GTCXTileDynamoHatch dynamoHatch = (GTCXTileDynamoHatch) world.getTileEntity(dynamo);
                double baseGeneration = getBaseGeneration();
                production = (int)(baseGeneration * getRotorEfficiency(this.getStackInSlot(0)));
                int fluidAmount = 1000 / getBaseDivider();
                if (inputHatch.getTank().getFluidAmount() >= fluidAmount && dynamoHatch.getStoredEnergy() + production <= dynamoHatch.getMaxEnergyStorage()){
                    if (!this.getActive()){
                        this.setActive(true);
                        this.setRingActive(true);
                    }
                    inputHatch.getTank().drainInternal(fluidAmount, true);
                    dynamoHatch.addEnergy(production);
                    if (ticker >= 80){
                        this.getStackInSlot(0).attemptDamageItem(1, world.rand, null);
                        ticker = 0;
                    }
                } else if (world.getTileEntity(input2) instanceof GTCXTileInputHatch){
                    GTCXTileInputHatch inputHatch2 = (GTCXTileInputHatch) world.getTileEntity(input2);
                    if (inputHatch2.getTank().getFluidAmount() >= fluidAmount && dynamoHatch.getStoredEnergy() + production <= dynamoHatch.getMaxEnergyStorage()){
                        if (!this.getActive()){
                            this.setActive(true);
                            this.setRingActive(true);
                        }
                        inputHatch2.getTank().drainInternal(fluidAmount, true);
                        dynamoHatch.addEnergy(production);
                        if (ticker >= 80){
                            this.getStackInSlot(0).attemptDamageItem(1, world.rand, null);
                            ticker = 0;
                        }
                    } else {
                        if (this.getActive()){
                            this.setActive(false);
                            this.setRingActive(false);
                        }
                    }
                } else {
                    if (this.getActive()){
                        this.setActive(false);
                        this.setRingActive(false);
                    }
                }
            } else {
                if (production != 0){
                    production = 0;
                }
                if (this.getActive()){
                    this.setActive(false);
                    this.setRingActive(false);
                }
            }
        } else {
            if (production != 0){
                production = 0;
            }
            if (this.getActive()){
                this.setActive(false);
                this.setRingActive(false);
            }
        }
    }

    public MultiRecipe getRecipe(){
        if (lastRecipe == GTRecipeMultiInputList.INVALID_RECIPE) {
            return null;
        }
        // Check if previous recipe is valid
        GTCXTileInputHatch hatch1 = (GTCXTileInputHatch) world.getTileEntity(input1);
        FluidStack input1 = hatch1.getTank().getFluid();
        TileEntity tile = world.getTileEntity(input2);
        GTCXTileInputHatch hatch2 = tile instanceof GTCXTileInputHatch ? (GTCXTileInputHatch) tile : null;
        FluidStack input2 = hatch2 != null ? hatch2.getTank().getFluid() : null;
        if (lastRecipe != null) {
            lastRecipe = checkRecipe(lastRecipe, input1) || (input2 != null && checkRecipe(lastRecipe, input2)) ? lastRecipe : null;
        }
        // If previous is not valid, find a new one
        if (lastRecipe == null) {
            lastRecipe = getRecipeList().getPriorityRecipe(new Predicate<MultiRecipe>() {

                @Override
                public boolean test(MultiRecipe t) {
                    return checkRecipe(t, input1);
                }
            });
            // if no recipe is found in input1 but input2 is not null, check input2
            if (lastRecipe == null && input2 != null){
                lastRecipe = getRecipeList().getPriorityRecipe(new Predicate<MultiRecipe>() {

                    @Override
                    public boolean test(MultiRecipe t) {
                        return checkRecipe(t, input2);
                    }
                });
            }
        }
        // If no recipe is found, return
        if (lastRecipe == null) {
            return null;
        }
        return lastRecipe;
    }

    public boolean checkRecipe(GTRecipeMultiInputList.MultiRecipe entry, FluidStack input) {
        IRecipeInput recipeInput = entry.getInput(0);
        if (recipeInput instanceof RecipeInputFluid){
            return input != null && input.isFluidEqual(((RecipeInputFluid)recipeInput).fluid);
        }
        return false;
    }

    public GTRecipeMultiInputList getRecipeList(){
        return GTCXRecipeLists.GAS_TURBINE_RECIPE_LIST;
    }

    public int getBaseDivider(){
        return getTotalRecipeEu() > 40960 ? 25 : 20;
    }

    public int getTotalRecipeEu(){
        return getRecipeEu(lastRecipe.getOutputs()) * getRecipeTicks(lastRecipe.getOutputs());
    }

    public double getBaseGeneration(){
        return (double) getTotalRecipeEu() / getBaseDivider();
    }

    public void setRingActive(boolean active){
        int3 dir = new int3(getPos(), getFacing());
        setCasingActive(dir.up(1), active);
        setCasingActive(dir.right(1), active);
        setCasingActive(dir.down(1), active);
        setCasingActive(dir.down(1), active);
        setCasingActive(dir.left(1), active);
        setCasingActive(dir.left(1), active);
        setCasingActive(dir.up(1), active);
        setCasingActive(dir.up(1), active);
    }

    public void setCasingActive(int3 dir, boolean active){
        TileEntity tile = world.getTileEntity(dir.asBlockPos());
        if (tile instanceof GTCXTileCasing){
            GTCXTileCasing casing = (GTCXTileCasing) tile;
            if (casing.getActive() != active){
                casing.setActive(active);
            }
        }
    }

    public float getRotorEfficiency(ItemStack stack){
        if (stack.getItem() == GTCXItems.bronzeTurbineRotor){
            return 0.6F;
        }
        if (stack.getItem() == GTCXItems.steelTurbineRotor){
            return 0.8F;
        }
        if (stack.getItem() == GTCXItems.magnaliumTurbineRotor){
            return 1.0F;
        }
        if (stack.getItem() == GTCXItems.tungstensteelTurbineRotor){
            return 0.9F;
        }
        if (stack.getItem() == GTCXItems.carbonTurbineRotor){
            return 1.25F;
        }
        return 0.0F;
    }

    @Override
    public boolean canRemoveBlock(EntityPlayer player) {
        return true;
    }

    @Override
    public ContainerIC2 getGuiContainer(EntityPlayer entityPlayer) {
        return new GTCXContainerLargeGasTurbine(entityPlayer.inventory, this);
    }

    @Override
    public Class<? extends GuiScreen> getGuiClass(EntityPlayer entityPlayer) {
        return GuiComponentContainer.class;
    }

    @Override
    public void onGuiClosed(EntityPlayer entityPlayer) {

    }

    @Override
    public boolean canInteractWith(EntityPlayer entityPlayer) {
        return !this.isInvalid();
    }

    @Override
    public boolean hasGui(EntityPlayer entityPlayer) {
        return true;
    }

    public static int getRecipeTicks(MachineOutput output) {
        if (output == null || output.getMetadata() == null) {
            return 0;
        }
        return output.getMetadata().getInteger(RECIPE_TICKS);
    }

    public static int getRecipeEu(MachineOutput output) {
        if (output == null || output.getMetadata() == null) {
            return 0;
        }
        return output.getMetadata().getInteger(RECIPE_EU);
    }

    @Override
    public boolean getStructureValid() {
        return lastState;
    }

    int inputs = 0;
    int outputs = 0;
    public boolean checkStructure() {
        if (!this.world.isAreaLoaded(this.pos, 3)){
            return false;
        }
        inputs = 0;
        outputs = 0;
        int3 dir = new int3(getPos(), getFacing());
        if (!isReinforcedCasingWithSpecial(dir.up(1), 2)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.right(1), 3)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.down(1), 5)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.down(1), 8)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.left(1), 7)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.left(1), 6)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.up(1), 4)){
            return false;
        }
        if (!isReinforcedCasingWithSpecial(dir.up(1), 1)){
            return false;
        }

        int i;
        for (i = 0; i < 3; i++){
            if (!isReinforcedCasing(dir.back(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.right(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isReinforcedCasing(dir.forward(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.right(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isReinforcedCasing(dir.back(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.down(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isInputHatch(dir.forward(1))){
                return false;
            }
        }
        if (world.getBlockState(dir.left(1).asBlockPos()) != Blocks.AIR.getDefaultState()){
            return false;
        }
        if (world.getBlockState(dir.back(1).asBlockPos()) != Blocks.AIR.getDefaultState()){
            return false;
        }
        if (!isDynamoHatch(dir.back(1))){
            return false;
        }
        if (!isReinforcedCasing(dir.left(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isInputHatch(dir.forward(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.down(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isReinforcedCasing(dir.back(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.right(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isInputHatch(dir.forward(1))){
                return false;
            }
        }
        if (!isReinforcedCasing(dir.right(1))){
            return false;
        }
        for (i = 0; i < 2; i++){
            if (!isReinforcedCasing(dir.back(1))){
                return false;
            }
        }

        if (inputs < 1){
            return false;
        }
        TileEntity tile = world.getTileEntity(input1);
        if (tile instanceof GTCXTileInputHatch && ((GTCXTileInputHatch)tile).getOwner() == null){
            ((GTCXTileInputHatch)tile).setOwner(this);
        }
        tile = world.getTileEntity(input2);
        if (tile instanceof GTCXTileInputHatch && ((GTCXTileInputHatch)tile).getOwner() == null){
            ((GTCXTileInputHatch)tile).setOwner(this);
        }
        return true;
    }

    public void removeRing(int3 dir){
        removeStandardCasingWithSpecial(dir.up(1));
        removeStandardCasingWithSpecial(dir.right(1));
        removeStandardCasingWithSpecial(dir.down(1));
        removeStandardCasingWithSpecial(dir.down(1));
        removeStandardCasingWithSpecial(dir.left(1));
        removeStandardCasingWithSpecial(dir.left(1));
        removeStandardCasingWithSpecial(dir.up(1));
        removeStandardCasingWithSpecial(dir.up(1));
        TileEntity tile = world.getTileEntity(input1);
        if (tile instanceof GTCXTileInputHatch && ((GTCXTileInputHatch)tile).getOwner() == this){
            ((GTCXTileInputHatch)tile).setOwner(null);
        }
        tile = world.getTileEntity(input2);
        if (tile instanceof GTCXTileInputHatch && ((GTCXTileInputHatch)tile).getOwner() == this){
            ((GTCXTileInputHatch)tile).setOwner(null);
        }
    }

    public void addRing(){
        int3 dir = new int3(this.pos, this.getFacing());
        setReinforcedCasingWithSpecial(dir.up(1), 2);
        setReinforcedCasingWithSpecial(dir.right(1), 3);
        setReinforcedCasingWithSpecial(dir.down(1), 5);
        setReinforcedCasingWithSpecial(dir.down(1), 8);
        setReinforcedCasingWithSpecial(dir.left(1), 7);
        setReinforcedCasingWithSpecial(dir.left(1), 6);
        setReinforcedCasingWithSpecial(dir.up(1), 4);
        setReinforcedCasingWithSpecial(dir.up(1), 1);
    }

    public boolean isReinforcedCasing(int3 pos) {
        return world.getBlockState(pos.asBlockPos()) == reinforcedCasingState;
    }

    public boolean isReinforcedCasingWithSpecial(int3 pos, int position) {
        IBlockState state = world.getBlockState(pos.asBlockPos());
        if (state == reinforcedCasingState){
            TileEntity tile = world.getTileEntity(pos.asBlockPos());
            if (tile instanceof GTCXTileCasing){
                GTCXTileCasing  casing = (GTCXTileCasing) tile;
                casing.setFacing(this.getFacing());
                casing.setRotor(position);
            }
            return true;
        }
        return false;
    }

    public void setReinforcedCasingWithSpecial(int3 pos, int position) {
        IBlockState state = world.getBlockState(pos.asBlockPos());
        if (state == reinforcedCasingState){
            TileEntity tile = world.getTileEntity(pos.asBlockPos());
            if (tile instanceof GTCXTileCasing){
                GTCXTileCasing  casing = (GTCXTileCasing) tile;
                casing.setFacing(this.getFacing());
                casing.setRotor(position);
            }
        }
    }

    public void removeStandardCasingWithSpecial(int3 pos) {
        IBlockState state = world.getBlockState(pos.asBlockPos());
        if (state == reinforcedCasingState){
            TileEntity tile = world.getTileEntity(pos.asBlockPos());
            if (tile instanceof GTCXTileCasing){
                ((GTCXTileCasing) tile).setRotor(0);
            }
        }
    }

    public boolean isInputHatch(int3 pos) {
        if (world.getBlockState(pos.asBlockPos()) == inputHatchState){
            if (world.getBlockState(input1) != inputHatchState){
                input1 = pos.asBlockPos();
            } else if (world.getBlockState(input2) != inputHatchState){
                input2 = pos.asBlockPos();
            }
            inputs++;
            return true;
        }
        return world.getBlockState(pos.asBlockPos()) == reinforcedCasingState;
    }
    public boolean isDynamoHatch(int3 pos) {
        if (world.getBlockState(pos.asBlockPos()) == dynamoHatchState){
            dynamo = pos.asBlockPos();
            return true;
        }
        return false;
    }

    @Override
    public void onNetworkEvent(int i) {
        if (i < 6){
            getNetwork().initiateClientTileEntityEvent(this, i);
        }
    }

    @Override
    public void onNetworkEvent(EntityPlayer entityPlayer, int i) {
        if (i < 5){
            getNetwork().initiateTileEntityEvent(this, i + 1, false);
        }
        if (i == 5){
            addRing();
        }
    }

    @Override
    public int getProduction() {
        return production;
    }

    @Override
    public void setShouldCheckRecipe(boolean checkRecipe) {
        shouldCheckRecipe = checkRecipe;
    }
}
