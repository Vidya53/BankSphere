import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export default function AppShell() {
  return (
    <div className="min-h-screen flex bg-accent-surface/40 dark:bg-ink-900">
      <Sidebar />
      <div className="flex-1 min-w-0 flex flex-col">
        <Topbar />
        <main className="flex-1 px-6 lg:px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
