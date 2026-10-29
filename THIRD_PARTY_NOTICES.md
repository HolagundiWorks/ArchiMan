# Third-party notices

## Carbon Icons

ArchiMan uses icon geometry from Carbon Design System's `@carbon/icons-react`
version 11.87.0 in both the Android application and its phone-hosted web
portal.

Copyright 2015 IBM Corp.

Licensed under the Apache License, Version 2.0. You may obtain a copy of the
license at <https://www.apache.org/licenses/LICENSE-2.0>.

The software is provided on an "AS IS" basis, without warranties or conditions
of any kind. The upstream project is available at
<https://github.com/carbon-design-system/carbon>.

## Archi Calc (AcharyaOS / Paperdesk calculator)

Siteman's embedded Archi Calc expression evaluator and length/area/volume
measure helper adapt logic from Holagundi Consulting Works' AcharyaOS /
Paperdesk calculator (`CalculatorEngine`, `UnitConverter`).

The standalone `HolagundiWorks/architect-calculator` repository was not
available as a cloneable module at integration time; the engines are ported
in-process under `com.example.domain.archicalc` and remain quantity-only
(no rates or commercial fields).

Licensed under the Apache License, Version 2.0.
https://www.apache.org/licenses/LICENSE-2.0
