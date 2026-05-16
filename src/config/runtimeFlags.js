const rawDeliveryProgressFlag = import.meta.env.VITE_ENABLE_DELIVERY_PROGRESS
const rawReferencePagesFlag = import.meta.env.VITE_ENABLE_REFERENCE_PAGES

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

export const referencePagesFlagState =
  rawReferencePagesFlag === 'true'
    ? 'enabled'
    : rawReferencePagesFlag === 'false'
      ? 'disabled'
      : 'default'

export const referencePagesEnabled =
  !isProductionRuntime && referencePagesFlagState !== 'disabled'

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
