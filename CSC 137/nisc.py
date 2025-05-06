import sys

OPCODES = {
    "ADD": 0, "SUB": 1, "AND": 2, "OR": 3,
    "NOT": (4, 0), "LSL": (4, 1), "LSR": (4, 2), "ASL": (4, 3),
    "MOVRF": (4, 4), "MOVFR": (4, 5),
    "MOVC": 6, "BNZ": 7,
}

REGS = {f"R{i}": i for i in range(8)}

def parse_line(line):
    line = line.split(";", 1)[0].split("#", 1)[0].strip()
    if not line:
        return None, None
    if ":" in line:
        label, rest = line.split(":", 1)
        return label.strip().upper(), rest.strip()
    return None, line

def passOne(lines):
    labels = {}
    cleaned = []
    addr = 0
    for lineno, line in enumerate(lines):
        label, instr = parse_line(line)
        if label:
            labels[label.upper()] = addr
        if instr:
            cleaned.append((lineno, instr))
            addr += 1
    return labels, cleaned

def encode(instr, labels, pc):
    parts = instr.upper().split()
    if not parts:
        return None
    mnemonic = parts[0]

    if mnemonic in {"ADD", "SUB", "AND", "OR"}:
        rd, rn, rm = (REGS[p] for p in parts[1:])
        op = OPCODES[mnemonic]
        return (op << 9) | (rm << 6) | (rn << 3) | rd

    elif mnemonic in {"NOT", "LSL", "LSR", "ASR"}:
        rd, rn = REGS[parts[1]], REGS[parts[2]]
        op, subop = OPCODES[mnemonic]
        return (op << 9) | (subop << 6) | (rn << 3) | rd

    elif mnemonic == "MOVRF":
        # Source: Rn
        rn = REGS[parts[1]]
        rd = 0  # dummy, not used
        op, subop = OPCODES[mnemonic]
        return (op << 9) | (subop << 6) | (rn << 3) | rd

    elif mnemonic == "MOVFR":
        # Destination: Rd
        rd = REGS[parts[1]]
        rn = 0  # dummy, not used
        op, subop = OPCODES[mnemonic]
        return (op << 9) | (subop << 6) | (rn << 3) | rd

    elif mnemonic == "MOVC":
        rd = REGS[parts[1]]
        imm = int(parts[2])
        if not -32 <= imm < 32:
            raise ValueError("Immediate out of range (-32 to 31)")
        if imm < 0:
            imm = (1 << 6) + imm
        return (6 << 9) | (imm << 3) | rd

    elif mnemonic == "BNZ":
        target = parts[1]
        bta = int(target) if target.isdigit() else labels[target.upper()]
        return (7 << 9) | bta

    raise ValueError(f"Unknown instruction: {mnemonic}")

def passTwo(cleaned, labels):
    yield "v2.0 raw"
    for pc, (lineno, instr) in enumerate(cleaned):
        try:
            code = encode(instr, labels, pc)
            if code is not None:
                yield f"{code:03x}"
        except Exception as e:
            raise RuntimeError(f"Error on line {lineno + 1}: {instr}\n{e}")

if __name__ == "__main__":
    with open(sys.argv[1]) as f:
        lines = f.readlines()
    labels, cleaned = passOne(lines)
    for line in passTwo(cleaned, labels):
        print(line)
