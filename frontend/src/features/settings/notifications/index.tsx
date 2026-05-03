import { ContentSection } from '../components/content-section'
import { NotificationsForm } from './notifications-form'

export function SettingsNotifications() {
  return (
    <ContentSection
      title='通知'
      desc='管理你希望接收的消息类型与提醒方式。'
    >
      <NotificationsForm />
    </ContentSection>
  )
}
