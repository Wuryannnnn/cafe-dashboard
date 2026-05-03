import { Card, CardContent } from '@/components/ui/card'

export type Column<T> = {
  key?: string
  header: React.ReactNode
  render: (row: T) => React.ReactNode
  align?: 'left' | 'right' | 'center'
  className?: string
}

type Props<T> = {
  columns: Column<T>[]
  rows?: T[]
  loading?: boolean
  emptyText?: string
  rowKey: (row: T) => string | number
}

export function SimpleTable<T>({ columns, rows, loading, emptyText = '暂无数据', rowKey }: Props<T>) {
  return (
    <Card>
      <CardContent className='p-0'>
        <div className='overflow-x-auto'>
          <table className='w-full text-sm'>
            <thead>
              <tr className='text-muted-foreground border-b text-xs uppercase tracking-wide'>
                {columns.map((c, i) => (
                  <th
                    key={i}
                    className={
                      'p-3 font-medium ' +
                      (c.align === 'right' ? 'text-right' : c.align === 'center' ? 'text-center' : 'text-left') +
                      (c.className ? ' ' + c.className : '')
                    }
                  >
                    {c.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={columns.length} className='text-muted-foreground p-12 text-center'>
                    加载中…
                  </td>
                </tr>
              )}
              {!loading && (!rows || rows.length === 0) && (
                <tr>
                  <td colSpan={columns.length} className='text-muted-foreground p-12 text-center'>
                    {emptyText}
                  </td>
                </tr>
              )}
              {rows?.map((row) => (
                <tr key={rowKey(row)} className='hover:bg-muted/50 border-b last:border-0'>
                  {columns.map((c, i) => (
                    <td
                      key={i}
                      className={
                        'p-3 ' +
                        (c.align === 'right' ? 'text-right' : c.align === 'center' ? 'text-center' : 'text-left') +
                        (c.className ? ' ' + c.className : '')
                      }
                    >
                      {c.render(row)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  )
}
