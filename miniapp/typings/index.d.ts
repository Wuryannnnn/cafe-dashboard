// 轻量声明: 不引 npm 的 miniprogram-api-typings, 让原生 TS 直接编译.
// 上生产可换成 `npm i -D miniprogram-api-typings` 获得完整类型提示.
declare const wx: any
declare const App: (opts: any) => void
declare const Page: (opts: any) => void
declare const Component: (opts: any) => void
declare const getApp: () => any
declare const getCurrentPages: () => any[]

// 小程序运行时全局定时器 (ES2018 lib 不含, 这里声明)
declare function setInterval(handler: () => void, timeout?: number): number
declare function clearInterval(id: number): void
declare function setTimeout(handler: () => void, timeout?: number): number
declare function clearTimeout(id: number): void
