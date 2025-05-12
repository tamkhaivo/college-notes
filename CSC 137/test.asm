; Test file for MOVC instruction
; NISC CPU
; Operands are space-separated

; Test loading a positive value
    MOVC R0 5       ; R0 = 5 (0x005)
    MOVC R1 31      ; R1 = 31 (0x01F, max positive 6-bit signed)

; Test loading zero
    MOVC R2 0       ; R2 = 0

; Test loading a negative value
    MOVC R3 -1      ; R3 = -1 (0xFFF from 6-bit 0b111111)
    MOVC R4 -32     ; R4 = -32 (0xFE0 from 6-bit 0b100000, max negative 6-bit signed)

; Load a value into R0 to signal test completion (observe R0 in simulator)
    MOVC R0 10      ; Signal MOVC test sequence potentially done

HALT_MOVC:
    BNZ HALT_MOVC   ; Infinite loop
