enum Option<T> { Some(T), None }
use Option::*;

fn let_else(o: Option<i32>) -> i32 {
    let Some(v) = o else {
        return 0;
    };
    v
}

fn while_let(mut o: Option<i32>) {
    while let Some(v) = o {
        o = None;
        let _ = v;
    }
}

fn uninit(c: bool) -> i32 {
    let x;
    if c {
        x = 1;
    } else {
        x = 2;
    }
    x
}
