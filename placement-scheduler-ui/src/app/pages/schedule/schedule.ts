import {
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  FormsModule
} from '@angular/forms';

import {
  finalize,
  timeout
} from 'rxjs';

import {
  ScheduleService,
  ScheduleGenerationResult
} from '../../services/schedule';

import {
  ScheduleItem
} from '../../models/schedule.model';


@Component({
  selector: 'app-schedule',

  imports: [
    CommonModule,
    FormsModule
  ],

  templateUrl: './schedule.html',

  styleUrl: './schedule.css'
})
export class ScheduleComponent
    implements OnInit {


  private readonly scheduleService =
      inject(ScheduleService);


  schedules: ScheduleItem[] = [];


  /*
   * --------------------------------------------------
   * UI STATE
   * --------------------------------------------------
   */

  loading = false;

  generating = false;

  error = false;


  /*
   * --------------------------------------------------
   * FILTER STATE
   * --------------------------------------------------
   */

  selectedDate = '';

  selectedCompanyId:
      number | null = null;

  selectedStudentId:
      number | null = null;

  selectedRoomId:
      number | null = null;

  selectedPanelId:
      number | null = null;


  /*
   * --------------------------------------------------
   * MESSAGES
   * --------------------------------------------------
   */

  generationMessage = '';


  /*
   * --------------------------------------------------
   * INITIAL LOAD
   * --------------------------------------------------
   */

  ngOnInit(): void {

    /*
     * Load the existing schedule silently.
     *
     * The user should immediately see the schedule
     * if it already exists.
     */
    this.loadSchedule(false);
  }


  /*
   * --------------------------------------------------
   * LOAD SCHEDULE
   * --------------------------------------------------
   */

  loadSchedule(
      showLoading = true
  ): void {

    /*
     * Never start a GET while schedule generation
     * is running.
     */
    if (this.generating) {
      return;
    }


    if (showLoading) {

      this.loading = true;
    }


    this.error = false;


    this.scheduleService
        .getSchedule({

          date:
              this.selectedDate || undefined,

          companyId:
              this.selectedCompanyId ?? undefined,

          studentId:
              this.selectedStudentId ?? undefined,

          roomId:
              this.selectedRoomId ?? undefined,

          panelId:
              this.selectedPanelId ?? undefined

        })
        .pipe(

          /*
           * Prevent a stuck loading state.
           */
          timeout(10000),

          finalize(() => {

            if (showLoading) {

              this.loading = false;
            }

          })

        )
        .subscribe({

          next: (
              data: ScheduleItem[]
          ) => {

            console.log(
                'SCHEDULE: loaded',
                data.length,
                'interviews'
            );


            this.schedules = data;

            this.error = false;
          },


          error: (err) => {

            console.error(
                'SCHEDULE: load failed',
                err
            );


            /*
             * Keep existing rows visible if a refresh
             * fails.
             */
            this.error = true;
          }

        });
  }


  /*
   * --------------------------------------------------
   * APPLY FILTERS
   * --------------------------------------------------
   */

  applyFilters(): void {

    if (
        this.loading ||
        this.generating
    ) {

      return;
    }


    this.generationMessage = '';

    this.loadSchedule(true);
  }


  /*
   * --------------------------------------------------
   * CLEAR FILTERS
   * --------------------------------------------------
   */

  clearFilters(): void {

    if (
        this.loading ||
        this.generating
    ) {

      return;
    }


    this.selectedDate = '';

    this.selectedCompanyId = null;

    this.selectedStudentId = null;

    this.selectedRoomId = null;

    this.selectedPanelId = null;


    this.generationMessage = '';

    this.error = false;


    /*
     * Load the complete schedule again.
     */
    this.loadSchedule(true);
  }


  /*
   * --------------------------------------------------
   * REFRESH
   * --------------------------------------------------
   */

  refresh(): void {

    if (
        this.loading ||
        this.generating
    ) {

      return;
    }


    this.generationMessage = '';

    this.loadSchedule(true);
  }


  /*
   * --------------------------------------------------
   * GENERATE SCHEDULE
   * --------------------------------------------------
   */

  generateSchedule(): void {

    if (
        this.generating ||
        this.loading
    ) {

      return;
    }


    console.log(
        'SCHEDULE: generation started'
    );


    this.generating = true;

    this.error = false;

    this.generationMessage = '';


    this.scheduleService
        .generateSchedule()
        .pipe(

          /*
           * Schedule generation may take some time.
           */
          timeout(120000),

          /*
           * Always return the button to its normal
           * state.
           */
          finalize(() => {

            this.generating = false;

            console.log(
                'SCHEDULE: generation finished'
            );
          })

        )
        .subscribe({

          next: (
              response: ScheduleGenerationResult
          ) => {

            console.log(
                'SCHEDULE: generation response',
                response
            );


            this.generationMessage =
                response.message ||
                'Schedule generated successfully.';


            /*
             * Reload using the currently selected
             * filters.
             */
            this.loadSchedule(false);
          },


          error: (err) => {

            console.error(
                'SCHEDULE: generation failed',
                err
            );


            this.error = true;


            if (
                err?.name === 'TimeoutError'
            ) {

              this.generationMessage =
                  'Schedule generation timed out.';
            } else {

              this.generationMessage =
                  'Unable to generate schedule.';
            }

          }

        });
  }


  /*
   * --------------------------------------------------
   * FILTER COUNT
   * --------------------------------------------------
   */

  get activeFilterCount(): number {

    let count = 0;


    if (this.selectedDate) {
      count++;
    }

    if (this.selectedCompanyId != null) {
      count++;
    }

    if (this.selectedStudentId != null) {
      count++;
    }

    if (this.selectedRoomId != null) {
      count++;
    }

    if (this.selectedPanelId != null) {
      count++;
    }


    return count;
  }


  /*
   * --------------------------------------------------
   * RESULT COUNTS
   * --------------------------------------------------
   */

  get scheduledCount(): number {

    return this.schedules.filter(
        item =>
            item.status === 'SCHEDULED'
    ).length;
  }


  get unscheduledCount(): number {

    return this.schedules.filter(
        item =>
            item.status === 'UNSCHEDULED'
    ).length;
  }


  /*
   * --------------------------------------------------
   * TRACKING
   * --------------------------------------------------
   */

  trackByInterviewId(
      index: number,
      item: ScheduleItem
  ): number {

    return item.interviewId;
  }

}