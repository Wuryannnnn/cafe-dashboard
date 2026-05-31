import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, type RenderResult } from 'vitest-browser-react'
import { type Locator, userEvent } from 'vitest/browser'
import { UserAuthForm } from './user-auth-form'

const FORM_MESSAGES = {
  usernameEmpty: '请输入用户名',
  passwordEmpty: '请输入密码',
} as const

const navigate = vi.fn()
const setUserMock = vi.fn()
const setAccessTokenMock = vi.fn()
const postMock = vi.fn()

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: () => ({
    auth: {
      setUser: setUserMock,
      setAccessToken: setAccessTokenMock,
    },
  }),
}))

vi.mock('@/lib/api', () => ({
  api: { post: (...args: unknown[]) => postMock(...args) },
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useNavigate: () => navigate,
    Link: ({
      children,
      to,
      className,
      ...rest
    }: {
      children?: React.ReactNode
      to: string
      className?: string
    }) => (
      <a href={to} className={className} {...rest}>
        {children}
      </a>
    ),
  }
})

describe('UserAuthForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    postMock.mockResolvedValue({
      data: { code: 0, data: { username: 'boss', name: '张老板', role: 0 } },
    })
  })

  describe('Rendering without redirectTo', () => {
    let screen: RenderResult
    let usernameInput: Locator
    let passwordInput: Locator
    let signInButton: Locator
    let forgotPasswordLink: Locator

    beforeEach(async () => {
      screen = await render(<UserAuthForm />)
      usernameInput = screen.getByRole('textbox', { name: /用户名/i })
      passwordInput = screen.getByLabelText(/^密码$/i)
      signInButton = screen.getByRole('button', { name: /^登录$/i })
      forgotPasswordLink = screen.getByText(/^Forgot password\?$/i)
    })

    it('renders fields, submit button, and forgot password link', async () => {
      await expect.element(usernameInput).toBeInTheDocument()
      await expect.element(passwordInput).toBeInTheDocument()
      await expect.element(signInButton).toBeInTheDocument()
      await expect.element(forgotPasswordLink).toBeInTheDocument()
    })

    it('shows validation messages when submitting empty form', async () => {
      await userEvent.click(signInButton)

      await expect
        .element(screen.getByText(FORM_MESSAGES.usernameEmpty))
        .toBeInTheDocument()
      await expect
        .element(screen.getByText(FORM_MESSAGES.passwordEmpty))
        .toBeInTheDocument()
    })

    it('logs in via /api/admin/login and navigates to default route on success', async () => {
      await userEvent.fill(usernameInput, 'boss')
      await userEvent.fill(passwordInput, 'boss123')

      await userEvent.click(signInButton)

      await vi.waitFor(() => expect(setUserMock).toHaveBeenCalledOnce())
      expect(postMock).toHaveBeenCalledWith(
        '/api/admin/login',
        expect.any(URLSearchParams)
      )
      expect(setUserMock).toHaveBeenCalledWith(
        expect.objectContaining({
          email: 'boss',
          accountNo: 'boss',
          role: ['老板'],
          exp: expect.any(Number),
        })
      )
      expect(setAccessTokenMock).toHaveBeenCalledWith('cookie')

      await vi.waitFor(() =>
        expect(navigate).toHaveBeenCalledWith({ to: '/', replace: true })
      )
    })

    it('does not authenticate on wrong credentials', async () => {
      postMock.mockResolvedValue({ data: { code: 1, msg: '用户名或密码错误' } })

      await userEvent.fill(usernameInput, 'boss')
      await userEvent.fill(passwordInput, 'wrong')
      await userEvent.click(signInButton)

      await vi.waitFor(() => expect(postMock).toHaveBeenCalledOnce())
      expect(setUserMock).not.toHaveBeenCalled()
      expect(navigate).not.toHaveBeenCalled()
    })
  })

  it('navigates to redirectTo when provided', async () => {
    const { getByRole, getByLabelText } = await render(
      <UserAuthForm redirectTo='/settings' />
    )

    await userEvent.fill(getByRole('textbox', { name: /用户名/i }), 'boss')
    await userEvent.fill(getByLabelText(/^密码$/i), 'boss123')

    await userEvent.click(getByRole('button', { name: /^登录$/i }))

    await vi.waitFor(() => expect(setUserMock).toHaveBeenCalledOnce())
    expect(setAccessTokenMock).toHaveBeenCalledOnce()

    await vi.waitFor(() =>
      expect(navigate).toHaveBeenCalledWith({
        to: '/settings',
        replace: true,
      })
    )
  })
})
