import { ContentSection } from '../components/content-section'
import { AppearanceForm } from './appearance-form'

export function SettingsAppearance() {
  return (
    <ContentSection
      title='外观'
      desc='调整界面主题与字体，可自动随系统切换日间 / 夜间模式。'
    >
      <AppearanceForm />
    </ContentSection>
  )
}
