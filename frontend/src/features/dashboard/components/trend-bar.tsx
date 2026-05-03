import {
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

type Props = {
  labels: string[]
  values: (number | string)[]
}

export function TrendBar({ labels, values }: Props) {
  const data = labels.map((name, i) => ({
    name,
    total: Number(values[i] ?? 0),
  }))

  if (data.length === 0) {
    return (
      <div className='text-muted-foreground py-12 text-center text-sm'>暂无数据</div>
    )
  }

  const maxVal = Math.max(...data.map((d) => d.total), 0)
  // 给最高点上方留 ~20% 余量, 避免柱子顶到天花板; 全 0 时给一个最小刻度
  const yMax = maxVal === 0 ? 10 : Math.ceil((maxVal * 1.2) / 10) * 10

  return (
    <ResponsiveContainer width='100%' height={300}>
      <BarChart data={data} margin={{ top: 24, right: 12, left: 0, bottom: 8 }}>
        <CartesianGrid strokeDasharray='3 3' stroke='#e5e7eb' vertical={false} />
        <XAxis
          dataKey='name'
          stroke='#6b7280'
          fontSize={12}
          tickLine={false}
          axisLine={false}
          tick={{ fill: '#6b7280' }}
        />
        <YAxis
          stroke='#6b7280'
          fontSize={12}
          tickLine={false}
          axisLine={false}
          tick={{ fill: '#6b7280' }}
          tickFormatter={(v) => `¥${v}`}
          width={56}
          domain={[0, yMax]}
          allowDecimals={false}
        />
        <Tooltip
          cursor={{ fill: 'rgba(0,0,0,0.04)' }}
          contentStyle={{
            background: '#ffffff',
            border: '1px solid #e5e7eb',
            borderRadius: 8,
            fontSize: 12,
          }}
          formatter={(v: any) => [`¥${v}`, '营业额'] as [string, string]}
        />
        <Bar dataKey='total' radius={[6, 6, 0, 0]} maxBarSize={48} className='fill-primary'>
          <LabelList
            dataKey='total'
            position='top'
            formatter={(v: any) => (typeof v === 'number' && v > 0 ? `¥${v}` : '')}
            style={{ fill: '#374151', fontSize: 11, fontWeight: 600 }}
          />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
