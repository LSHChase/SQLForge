export const CAPABILITY_PLACEHOLDERS = Object.freeze({
  ACCESS_STRATEGY_CREATE: Object.freeze({
    titleKey: 'inline.viewsAccessCenterAccessCenterView.text016',
    capabilityKey: 'inline.viewsAccessCenterAccessCenterView.text017',
    reasonKey: 'inline.viewsAccessCenterAccessCenterView.text018',
    nextStepKey: 'inline.viewsAccessCenterAccessCenterView.text019'
  }),
  ACCESS_STRATEGY_EDIT: Object.freeze({
    titleKey: 'inline.viewsAccessCenterAccessCenterView.text020',
    capabilityKey: 'inline.viewsAccessCenterAccessCenterView.text021',
    reasonKey: 'inline.viewsAccessCenterAccessCenterView.text022',
    nextStepKey: 'inline.viewsAccessCenterAccessCenterView.text023'
  }),
  DISPATCH_POLICY_EDIT: Object.freeze({
    titleKey: 'inline.viewsSystemUseSystemManagement.text015',
    capabilityKey: 'inline.viewsSystemUseSystemManagement.text015',
    reasonKey: 'inline.viewsSystemUseSystemManagement.text016',
    nextStepKey: 'inline.viewsSystemUseSystemManagement.text017'
  })
})

export const resolveCapabilityPlaceholder = (placeholderKey, translate) => {
  const config = CAPABILITY_PLACEHOLDERS[placeholderKey] || {}
  const t = typeof translate === 'function' ? translate : key => key
  return {
    title: config.titleKey ? t(config.titleKey) : '',
    capability: config.capabilityKey ? t(config.capabilityKey) : '',
    reason: config.reasonKey ? t(config.reasonKey) : '',
    nextStep: config.nextStepKey ? t(config.nextStepKey) : ''
  }
}
