import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardHeader } from '../common/Card';
import { formatCompactINR, formatINR } from '../../utils/format';
import { useChartTheme, tooltipStyle, labelStyle } from './chartTheme';

export function BarTrend({
  data,
  title,
  subtitle,
  dataKeys = ['value'],
  colors,
  formatCurrency = false,
  stacked = false,
  height = 280,
}) {
  const t = useChartTheme();
  const palette = colors || t.series;
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} className="mb-4" />
      <div style={{ height }} className="-mx-2">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ left: 6, right: 6, top: 6, bottom: 0 }}>
            <CartesianGrid stroke={t.grid} vertical={false} />
            <XAxis dataKey={Object.keys(data?.[0] || {}).find((k) => !dataKeys.includes(k)) || 'label'}
                   tickLine={false} axisLine={false} fontSize={11} stroke={t.axis} />
            <YAxis tickLine={false} axisLine={false} fontSize={11} stroke={t.axis}
                   tickFormatter={formatCurrency ? formatCompactINR : undefined}
                   width={formatCurrency ? 60 : 36} />
            <Tooltip
              contentStyle={tooltipStyle(t)}
              labelStyle={labelStyle(t)}
              formatter={formatCurrency ? ((v) => [formatINR(v), '']) : undefined}
              cursor={{ fill: t.isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)' }}
            />
            {dataKeys.length > 1 && <Legend iconType="circle" wrapperStyle={{ fontSize: 12, color: t.label }} />}
            {dataKeys.map((k, i) => (
              <Bar
                key={k}
                dataKey={k}
                stackId={stacked ? 'a' : undefined}
                fill={palette[i % palette.length]}
                radius={stacked ? 0 : [6, 6, 0, 0]}
                maxBarSize={48}
              />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
