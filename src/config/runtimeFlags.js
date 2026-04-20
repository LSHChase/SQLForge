const rawDeliveryProgressFlag = import.meta.env.VITE_ENABLE_DELIVERY_PROGRESS

export const runtimeMode = import.meta.env.MODE
export const isProductionRuntime = runtimeMode === 'production'
export const deliveryProgressFlagState =
  rawDeliveryProgressFlag === 'true'
    ? 'enabled'
    : rawDeliveryProgressFlag === 'false'
      ? 'disabled'
      : 'default'

export const deliveryProgressEnabled =
  !isProductionRuntime && deliveryProgressFlagState !== 'disabled'

export const deliveryProgressAvailability = {
  enabled: deliveryProgressEnabled,
  mode: runtimeMode,
  flagState: deliveryProgressFlagState,
  visibilityReasonKey: isProductionRuntime
    ? 'deliveryProgress.runtime.reasonProduction'
    : deliveryProgressFlagState === 'disabled'
      ? 'deliveryProgress.runtime.reasonFlagDisabled'
      : 'deliveryProgress.runtime.reasonNonProduction'
}
