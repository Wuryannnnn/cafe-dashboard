import { ContentSection } from '../components/content-section'
import { DisplayForm } from './display-form'

export function SettingsDisplay() {
  return (
    <ContentSection
      title='显示'
      desc='控制应用各模块的显示与隐藏。'
    >
      <DisplayForm />
    </ContentSection>
  )
}
