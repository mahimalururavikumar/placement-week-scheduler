import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReplanHistory } from './replan-history';

describe('ReplanHistory', () => {
  let component: ReplanHistory;
  let fixture: ComponentFixture<ReplanHistory>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReplanHistory],
    }).compileComponents();

    fixture = TestBed.createComponent(ReplanHistory);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
