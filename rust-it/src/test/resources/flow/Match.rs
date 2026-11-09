enum E { A, B(i32), C { x: i32 } }

fn arms(e: E) -> i32 {
    match e {
        E::A => 0,
        E::B(v) => v,
        E::C { x } => x,
    }
}

fn guards(e: E) -> i32 {
    match e {
        E::B(v) if v > 0 => v,
        E::B(_) => 0,
        _ => -1,
    }
}

fn or_pats(n: i32) -> i32 {
    match n {
        1 | 2 => 1,
        3..=5 => 2,
        _ => 3,
    }
}

fn empty_match(e: Never) -> i32 {
    match e {}
}

enum Never {}
