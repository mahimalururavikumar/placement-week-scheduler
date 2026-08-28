import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Conflicts } from './conflicts';

describe('Conflicts', () => {
  let component: Conflicts;
  let fixture: ComponentFixture<Conflicts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Conflicts],
    }).compileComponents();

    fixture = TestBed.createComponent(Conflicts);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
