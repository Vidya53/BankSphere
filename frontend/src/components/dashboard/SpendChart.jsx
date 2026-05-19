import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { formatCompactINR, formatINR } from '../../utils/format';
import { Card, CardHeader } from '../common/Card';
import { useChartTheme, tooltipStyle, labelStyle } from './chartTheme';

export function SpendChart({ data, title = 'Spend trends', subtitle = 'Last 7 days' }) {
  const t = useChartTheme();
  const gradientId = t.isDark ? 'brandFillDark' : 'brandFillLight';
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} className="mb-4" />
      <div className="h-64 -mx-2">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ left: 6, right: 6, top: 6, bottom: 0 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%"   stopColor={t.areaFillStart} stopOpacity={t.areaFillOpacityStart} />
                <stop offset="100%" stopColor={t.areaFillStart} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid stroke={t.grid} vertical={false} />
            <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={11} stroke={t.axis} />
            <YAxis tickLine={false} axisLine={false} fontSize={11} stroke={t.axis} tickFormatter={formatCompactINR} width={50} />
            <Tooltip
              contentStyle={tooltipStyle(t)}
              labelStyle={labelStyle(t)}
              formatter={(v) => [formatINR(v), 'Amount']}
            />
            <Area type="monotone" dataKey="amount" stroke={t.areaStroke} strokeWidth={2.5} fill={`url(#${gradientId})`} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
