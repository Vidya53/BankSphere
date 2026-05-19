import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardHeader } from '../common/Card';
import { formatCompactINR, formatINR } from '../../utils/format';
import { useChartTheme, tooltipStyle, labelStyle } from './chartTheme';

export function LineTrend({
  data,
  title,
  subtitle,
  xKey = 'month',
  dataKeys = ['value'],
  colors,
  formatCurrency = false,
  height = 280,
}) {
  const t = useChartTheme();
  const palette = colors || t.series;
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} className="mb-4" />
      <div style={{ height }} className="-mx-2">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ left: 6, right: 6, top: 6, bottom: 0 }}>
            <CartesianGrid stroke={t.grid} vertical={false} />
            <XAxis dataKey={xKey} tickLine={false} axisLine={false} fontSize={11} stroke={t.axis} />
            <YAxis tickLine={false} axisLine={false} fontSize={11} stroke={t.axis}
                   tickFormatter={formatCurrency ? formatCompactINR : undefined}
                   width={formatCurrency ? 60 : 36} />
            <Tooltip
              contentStyle={tooltipStyle(t)}
              labelStyle={labelStyle(t)}
              formatter={formatCurrency ? ((v) => [formatINR(v), '']) : undefined}
            />
            {dataKeys.length > 1 && <Legend iconType="circle" wrapperStyle={{ fontSize: 12, color: t.label }} />}
            {dataKeys.map((k, i) => (
              <Line key={k} type="monotone" dataKey={k} stroke={palette[i % palette.length]}
                    strokeWidth={2.5} dot={{ r: 3 }} activeDot={{ r: 5 }} />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
