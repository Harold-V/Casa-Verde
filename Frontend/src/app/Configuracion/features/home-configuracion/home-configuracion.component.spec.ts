import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HomeConfiguracionComponent } from './home-configuracion.component';

describe('HomeConfiguracionComponent', () => {
  let component: HomeConfiguracionComponent;
  let fixture: ComponentFixture<HomeConfiguracionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeConfiguracionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeConfiguracionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
