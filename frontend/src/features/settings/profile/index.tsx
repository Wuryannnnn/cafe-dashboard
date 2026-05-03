import { ContentSection } from '../components/content-section'
import { ProfileForm } from './profile-form'

export function SettingsProfile() {
  return (
    <ContentSection
      title='个人资料'
      desc='这些信息将展示给系统中的其他人。'
    >
      <ProfileForm />
    </ContentSection>
  )
}
