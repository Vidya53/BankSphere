import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { Card, CardHeader } from '../common/Card';
import { formatINR } from '../../utils/format';
import { useChartTheme, tooltipStyle } from './chartTheme';

export function CategoryDonut({ data, title = 'Spend by category', subtitle = 'This month' }) {
  const t = useChartTheme();
  const total = data.reduce((s, d) => s + d.value, 0);
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} className="mb-4" />
      <div className="grid grid-cols-1 md:grid-cols-[200px_1fr] items-center gap-6">
        <div className="relative h-48">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Tooltip
                contentStyle={tooltipStyle(t)}
                formatter={(v, n) => [formatINR(v), n]}
              />
              <Pie data={data} dataKey="value" innerRadius={55} outerRadius={80} stroke="none" paddingAngle={3}>
                {data.map((_, i) => <Cell key={i} fill={t.series[i % t.series.length]} />)}
              </Pie>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 grid place-items-center pointer-events-none">
            <div className="text-center">
              <p className="text-[10px] uppercase tracking-widest text-accent-mute dark:text-ink-400">Total</p>
              <p className="font-display font-extrabold text-lg text-accent-ink dark:text-ink-100">{formatINR(total)}</p>
            </div>
          </div>
        </div>

        <ul className="space-y-2.5">
          {data.map((d, i) => {
            const pct = total ? (d.value / total) * 100 : 0;
            return (
              <li key={d.name} className="flex items-center justify-between gap-3 text-sm">
                <span className="flex items-center gap-2.5 min-w-0">
                  <span className="h-2.5 w-2.5 rounded-full shrink-0" style={{ background: t.series[i % t.series.length] }} />
                  <span className="text-accent-slate truncate dark:text-ink-300">{d.name}</span>
                </span>
                <span className="flex items-center gap-3 shrink-0">
                  <span className="text-xs text-accent-mute dark:text-ink-400">{pct.toFixed(0)}%</span>
                  <span className="font-semibold text-accent-ink dark:text-ink-100">{formatINR(d.value)}</span>
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    </Card>
  );
}
