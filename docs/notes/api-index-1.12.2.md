# IE 0.12-89 API 索引（1.12.2）

> 由脚本从源码自动生成，只列 public 类型；方法与字段请直接看源码。

## blusunrize.immersiveengineering.api

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `AdvancedAABB` | `AdvancedAABB.java` |
| class | `ApiUtils` | `ApiUtils.java` |
| class | `CapabilitySkyhookData` | `CapabilitySkyhookData.java` |
| class | `ComparableItemStack` | `ComparableItemStack.java` |
| class | `DimensionBlockPos` | `DimensionBlockPos.java` |
| class | `DimensionChunkCoords` | `DimensionChunkCoords.java` |
| class | `DirectionalBlockPos` | `DirectionalBlockPos.java` |
| class | `IEApi` | `IEApi.java` |
| class | `IEEnums` | `IEEnums.java` |
| class | `IEProperties` | `IEProperties.java` |
| interface | `IMultiblock` | `MultiblockHandler.java` |
| interface | `IPostBlock` | `IPostBlock.java` |
| class | `Lib` | `Lib.java` |
| class | `ManualHelper` | `ManualHelper.java` |
| class | `ManualPageBlueprint` | `ManualPageBlueprint.java` |
| class | `ManualPageMultiblock` | `ManualPageMultiblock.java` |
| class | `MultiblockFormEvent` | `MultiblockHandler.java` |
| class | `MultiblockHandler` | `MultiblockHandler.java` |
| class | `PropertyBoolInverted` | `IEProperties.java` |
| class | `PropertySet` | `IEProperties.java` |
| class | `ProperySideConfig` | `IEProperties.java` |
| enum | `SideConfig` | `IEEnums.java` |
| class | `SimpleSkyhookProvider` | `CapabilitySkyhookData.java` |
| enum | `SkyhookStatus` | `CapabilitySkyhookData.java` |
| class | `SkyhookUserData` | `CapabilitySkyhookData.java` |
| class | `TargetingInfo` | `TargetingInfo.java` |
| class | `ValueComparator` | `ApiUtils.java` |

## blusunrize.immersiveengineering.api.crafting

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `AlloyRecipe` | `crafting/AlloyRecipe.java` |
| class | `ArcFurnaceRecipe` | `crafting/ArcFurnaceRecipe.java` |
| class | `BlastFurnaceFuel` | `crafting/BlastFurnaceRecipe.java` |
| class | `BlastFurnaceRecipe` | `crafting/BlastFurnaceRecipe.java` |
| class | `BlueprintCraftingRecipe` | `crafting/BlueprintCraftingRecipe.java` |
| class | `BottlingMachineRecipe` | `crafting/BottlingMachineRecipe.java` |
| class | `CokeOvenRecipe` | `crafting/CokeOvenRecipe.java` |
| class | `CrusherRecipe` | `crafting/CrusherRecipe.java` |
| class | `FermenterRecipe` | `crafting/FermenterRecipe.java` |
| interface | `IJEIRecipe` | `crafting/IJEIRecipe.java` |
| interface | `IMultiblockRecipe` | `crafting/IMultiblockRecipe.java` |
| class | `IngredientStack` | `crafting/IngredientStack.java` |
| class | `MetalPressRecipe` | `crafting/MetalPressRecipe.java` |
| class | `MixerRecipe` | `crafting/MixerRecipe.java` |
| class | `MultiblockRecipe` | `crafting/MultiblockRecipe.java` |
| class | `RefineryRecipe` | `crafting/RefineryRecipe.java` |
| class | `SqueezerRecipe` | `crafting/SqueezerRecipe.java` |

## blusunrize.immersiveengineering.api.energy

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `DieselHandler` | `energy/DieselHandler.java` |
| interface | `IRotationAcceptor` | `energy/IRotationAcceptor.java` |
| class | `ThermoelectricHandler` | `energy/ThermoelectricHandler.java` |

## blusunrize.immersiveengineering.api.energy.immersiveflux

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `FluxStorage` | `energy/immersiveflux/FluxStorage.java` |
| class | `FluxStorageAdvanced` | `energy/immersiveflux/FluxStorageAdvanced.java` |
| interface | `IFluxConnection` | `energy/immersiveflux/IFluxConnection.java` |
| interface | `IFluxContainerItem` | `energy/immersiveflux/IFluxContainerItem.java` |
| interface | `IFluxProvider` | `energy/immersiveflux/IFluxProvider.java` |
| interface | `IFluxReceiver` | `energy/immersiveflux/IFluxReceiver.java` |
| interface | `IFluxStorage` | `energy/immersiveflux/IFluxStorage.java` |

## blusunrize.immersiveengineering.api.energy.wires

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `AbstractConnection` | `energy/wires/ImmersiveNetHandler.java` |
| class | `BlockPlaceListener` | `energy/wires/ImmersiveNetHandler.java` |
| class | `BlockWireInfo` | `energy/wires/ImmersiveNetHandler.java` |
| class | `Connection` | `energy/wires/ImmersiveNetHandler.java` |
| class | `FeedthroughModelInfo` | `energy/wires/WireApi.java` |
| class | `IICProxy` | `energy/wires/IICProxy.java` |
| interface | `IImmersiveConnectable` | `energy/wires/IImmersiveConnectable.java` |
| interface | `IWireCoil` | `energy/wires/IWireCoil.java` |
| class | `ImmersiveNetHandler` | `energy/wires/ImmersiveNetHandler.java` |
| class | `TileEntityImmersiveConnectable` | `energy/wires/TileEntityImmersiveConnectable.java` |
| class | `WireApi` | `energy/wires/WireApi.java` |
| class | `WireType` | `energy/wires/WireType.java` |

## blusunrize.immersiveengineering.api.energy.wires.redstone

| 类型 | 名称 | 文件 |
|---|---|---|
| interface | `IRedstoneConnector` | `energy/wires/redstone/IRedstoneConnector.java` |
| class | `RedstoneWireNetwork` | `energy/wires/redstone/RedstoneWireNetwork.java` |

## blusunrize.immersiveengineering.api.fluid

| 类型 | 名称 | 文件 |
|---|---|---|
| interface | `IFluidPipe` | `fluid/IFluidPipe.java` |

## blusunrize.immersiveengineering.api.shader

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `CapabilityShader` | `shader/CapabilityShader.java` |
| class | `DynamicShaderLayer` | `shader/ShaderCase.java` |
| interface | `IShaderEffectFunction` | `shader/IShaderEffectFunction.java` |
| interface | `IShaderItem` | `shader/IShaderItem.java` |
| interface | `IShaderRegistryMethod` | `shader/ShaderRegistry.java` |
| class | `ShaderCase` | `shader/ShaderCase.java` |
| class | `ShaderCaseBalloon` | `shader/ShaderCaseBalloon.java` |
| class | `ShaderCaseBanner` | `shader/ShaderCaseBanner.java` |
| class | `ShaderCaseChemthrower` | `shader/ShaderCaseChemthrower.java` |
| class | `ShaderCaseDrill` | `shader/ShaderCaseDrill.java` |
| class | `ShaderCaseItem` | `shader/ShaderCaseItem.java` |
| class | `ShaderCaseMinecart` | `shader/ShaderCaseMinecart.java` |
| class | `ShaderCaseRailgun` | `shader/ShaderCaseRailgun.java` |
| class | `ShaderCaseRevolver` | `shader/ShaderCaseRevolver.java` |
| class | `ShaderCaseShield` | `shader/ShaderCaseShield.java` |
| class | `ShaderLayer` | `shader/ShaderCase.java` |
| class | `ShaderRegistry` | `shader/ShaderRegistry.java` |
| class | `ShaderRegistryEntry` | `shader/ShaderRegistry.java` |
| class | `ShaderWrapper` | `shader/CapabilityShader.java` |
| class | `ShaderWrapper_Direct` | `shader/CapabilityShader.java` |
| class | `ShaderWrapper_Item` | `shader/CapabilityShader.java` |

## blusunrize.immersiveengineering.api.tool

| 类型 | 名称 | 文件 |
|---|---|---|
| class | `AssemblerHandler` | `tool/AssemblerHandler.java` |
| class | `BelljarHandler` | `tool/BelljarHandler.java` |
| class | `BulletHandler` | `tool/BulletHandler.java` |
| class | `ChemthrowerEffect` | `tool/ChemthrowerHandler.java` |
| class | `ChemthrowerEffect_Damage` | `tool/ChemthrowerHandler.java` |
| class | `ChemthrowerEffect_Extinguish` | `tool/ChemthrowerHandler.java` |
| class | `ChemthrowerEffect_Potion` | `tool/ChemthrowerHandler.java` |
| class | `ChemthrowerEffect_RandomTeleport` | `tool/ChemthrowerHandler.java` |
| class | `ChemthrowerHandler` | `tool/ChemthrowerHandler.java` |
| enum | `ConveyorDirection` | `tool/ConveyorHandler.java` |
| class | `ConveyorHandler` | `tool/ConveyorHandler.java` |
| class | `DamagingBullet` | `tool/BulletHandler.java` |
| class | `DefaultFurnaceAdapter` | `tool/ExternalHeaterHandler.java` |
| class | `DefaultPlantHandler` | `tool/BelljarHandler.java` |
| class | `ExcavatorHandler` | `tool/ExcavatorHandler.java` |
| class | `ExternalHeaterHandler` | `tool/ExternalHeaterHandler.java` |
| interface | `FluidFertilizerHandler` | `tool/BelljarHandler.java` |
| class | `HeatableAdapter` | `tool/ExternalHeaterHandler.java` |
| interface | `IBullet` | `tool/BulletHandler.java` |
| interface | `IConfigurableTool` | `tool/IConfigurableTool.java` |
| interface | `IConveyorAttachable` | `tool/ConveyorHandler.java` |
| interface | `IConveyorBelt` | `tool/ConveyorHandler.java` |
| interface | `IConveyorTile` | `tool/ConveyorHandler.java` |
| interface | `IDrillHead` | `tool/IDrillHead.java` |
| interface | `IElectricEquipment` | `tool/IElectricEquipment.java` |
| interface | `IExternalHeatable` | `tool/ExternalHeaterHandler.java` |
| interface | `IInternalStorageItem` | `tool/IInternalStorageItem.java` |
| interface | `IPlantHandler` | `tool/BelljarHandler.java` |
| interface | `IPlantRenderer` | `tool/BelljarHandler.java` |
| interface | `IRecipeAdapter` | `tool/AssemblerHandler.java` |
| interface | `ITeslaEntity` | `tool/ITeslaEntity.java` |
| interface | `ITeslaEquipment` | `tool/ITeslaEquipment.java` |
| interface | `ITool` | `tool/ITool.java` |
| interface | `IUpgrade` | `tool/IUpgrade.java` |
| interface | `IUpgradeableTool` | `tool/IUpgradeableTool.java` |
| interface | `IZoomTool` | `tool/ZoomHandler.java` |
| interface | `ItemFertilizerHandler` | `tool/BelljarHandler.java` |
| class | `MineralMix` | `tool/ExcavatorHandler.java` |
| class | `MineralSelection` | `tool/ExcavatorHandler.java` |
| class | `MineralWorldInfo` | `tool/ExcavatorHandler.java` |
| class | `RailgunHandler` | `tool/RailgunHandler.java` |
| class | `RailgunProjectileProperties` | `tool/RailgunHandler.java` |
| class | `RecipeQuery` | `tool/AssemblerHandler.java` |
| class | `ToolConfigBoolean` | `tool/IConfigurableTool.java` |
| class | `ToolConfigFloat` | `tool/IConfigurableTool.java` |
| class | `ToolboxHandler` | `tool/ToolboxHandler.java` |
| class | `ZoomHandler` | `tool/ZoomHandler.java` |
