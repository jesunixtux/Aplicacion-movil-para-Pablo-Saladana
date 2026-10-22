# Datos ficticios para pruebas manuales

En compilaciones **debug** estos datos y el contenido de ejemplo se agregan automáticamente al abrir la app si aún faltan. La operación no duplica ni sobrescribe cuentas o registros existentes. No se inicia sesión automáticamente: entra desde **Iniciar sesión** con el correo y la contraseña. En una compilación normal/release no se agregan cuentas de demostración.

> Son datos sintéticos. Los identificadores `DEMO-00X` son deliberadamente inválidos como RUT y solo se guardan mediante la semilla debug; no sirven para crear estas cuentas manualmente en el formulario. No uses estos registros en producción. La contraseña es pública y jamás debe reutilizarse.

## Contraseña común de prueba

`BitacoraDemo26!`

Cumple el mínimo de 8 caracteres. Escríbela igual en **Contraseña** y **Confirmar contraseña**.

## Usuario 1 — Docente

| Campo | Valor |
|---|---|
| Nombre | Camila |
| Segundo nombre | Andrea |
| Apellido paterno | Rojas |
| Apellido materno | Mella |
| Identificador de prueba | `DEMO-001` |
| Correo | `docente.prueba@example.com` |
| Rol | Docente |
| Género | Femenino |
| Contraseña | `BitacoraDemo26!` |

## Usuario 2 — Estudiante

| Campo | Valor |
|---|---|
| Nombre | Martín |
| Segundo nombre | (dejar vacío) |
| Apellido paterno | Soto |
| Apellido materno | Vera |
| Identificador de prueba | `DEMO-002` |
| Correo | `estudiante.prueba@example.com` |
| Rol | Estudiante |
| Género | Masculino |
| Contraseña | `BitacoraDemo26!` |

## Usuario 3 — Tercer estudiante

| Campo | Valor |
|---|---|
| Nombre | Alex |
| Segundo nombre | Paz |
| Apellido paterno | Fuentes |
| Apellido materno | León |
| Identificador de prueba | `DEMO-003` |
| Correo | `tercer.estudiante@example.com` |
| Rol | Estudiante |
| Género | Otro |
| Contraseña | `BitacoraDemo26!` |

## Cuenta creada manualmente en la prueba final

Esta cuenta existe solo en la instalación debug del emulador `emulator-5554`; no forma parte de la semilla automática. Si se borran los datos de esa instalación, hay que registrarla de nuevo.

| Campo | Valor |
|---|---|
| Nombre | Prueba |
| Apellido paterno | Tester |
| Apellido materno | Uno |
| RUT ficticio válido | `12.345.678-5` |
| Correo | `qa.final.20261022@example.com` |
| Rol | Estudiante |
| Género | Otro |
| Contraseña | `PruebaQA26!` |

## Contenido de ejemplo para probar bitácoras

La compilación debug crea esta bitácora publicada para la cuenta docente:

| Campo | Valor |
|---|---|
| Título | Demostración: ciclo del agua |
| Propósito | Reconocer las etapas principales del ciclo del agua y relacionarlas con el entorno. |
| Instrucciones | Lee el contenido de la clase. Luego registra qué actividad realizaste, qué aprendiste y cualquier duda. |
| Preguntas orientadoras | ¿Qué etapas tiene el ciclo del agua? ¿Dónde observas evaporación o condensación en tu vida diaria? |

También crea para cada estudiante un registro enviado y un borrador. Para volver a probar manualmente, inicia sesión como estudiante, abre la bitácora y usa estos datos:

| Campo | Valor |
|---|---|
| Fecha de la clase | `20/10/2026` |
| Contenido trabajado | Etapas del ciclo del agua: evaporación, condensación y precipitación. |
| Actividad realizada | Dibujé el ciclo del agua y relacioné cada etapa con un ejemplo cotidiano. |
| ¿Qué aprendí? | Aprendí que el agua cambia de estado y circula continuamente entre la superficie y la atmósfera. |
| Dudas o dificultades | Quiero entender mejor cómo influye la temperatura en la evaporación. |

## Pruebas rápidas

1. En una instalación debug nueva, inicia sesión con cada correo y la contraseña común.
2. Verifica que el docente vea la bitácora y que cada estudiante vea sus registros enviado y borrador.
3. Crea una bitácora adicional como docente y un registro adicional como estudiante.
4. Cierra y vuelve a abrir la app: los datos de demostración y los nuevos datos deben persistir sin duplicarse.
5. Intenta registrar otra cuenta con el mismo correo: debe avisar que ya existe.
6. Prueba una contraseña de menos de 8 caracteres y un RUT modificado: el formulario debe rechazarlos.

Para repetir desde cero, borra los datos únicamente de la instalación local debug: esa acción elimina las cuentas, fotos y bitácoras guardadas en ese dispositivo; al abrir de nuevo, se cargarán otra vez los datos de demostración.
