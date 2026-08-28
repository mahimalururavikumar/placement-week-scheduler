import { TestBed } from '@angular/core/testing';

import { Replan } from './replan';

describe('Replan', () => {
  let service: Replan;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Replan);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
