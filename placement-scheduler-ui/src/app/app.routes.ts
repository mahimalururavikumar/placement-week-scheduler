import { Routes } from '@angular/router';

import { DashboardComponent } from './pages/dashboard/dashboard';
import { ScheduleComponent } from './pages/schedule/schedule';
import { ConflictsComponent } from './pages/conflicts/conflicts';
import { ReplanHistoryComponent } from './pages/replan-history/replan-history';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },

  {
    path: 'dashboard',
    component: DashboardComponent
  },

  {
    path: 'schedule',
    component: ScheduleComponent
  },

  {
    path: 'conflicts',
    component: ConflictsComponent
  },

  {
    path: 'replan-history',
    component: ReplanHistoryComponent
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }
];