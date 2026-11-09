fn if_else(c: bool) -> i32 {
    if c { 1 } else { 2 }
}

fn if_no_else(c: bool) {
    let mut x = 0;
    if c {
        x = 1;
    }
    let _ = x;
}

fn else_if(a: i32) -> i32 {
    if a < 0 { -1 } else if a == 0 { 0 } else { 1 }
}

fn short_circuit(a: bool, b: bool) -> bool {
    a && b || !a
}

fn if_let(o: Option<i32>) -> i32 {
    if let Some(v) = o { v } else { 0 }
}

enum Option<T> { Some(T), None }
use Option::*;
