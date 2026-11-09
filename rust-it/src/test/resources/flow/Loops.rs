fn while_loop(mut n: i32) {
    while n > 0 {
        n -= 1;
    }
}

fn loop_break() -> i32 {
    let mut i = 0;
    loop {
        i += 1;
        if i == 10 {
            break;
        }
    }
    i
}

fn loop_break_value() -> i32 {
    loop {
        break 42;
    }
}

fn for_loop(xs: [i32; 3]) {
    for x in xs {
        if x == 0 {
            continue;
        }
        let _ = x;
    }
}

fn labeled() {
    'outer: loop {
        loop {
            break 'outer;
        }
    }
}

fn labeled_continue() {
    let mut i = 0;
    'outer: while i < 10 {
        i += 1;
        loop {
            continue 'outer;
        }
    }
}

fn infinite() -> i32 {
    loop {}
}

fn labeled_block(c: bool) -> i32 {
    'b: {
        if c {
            break 'b 1;
        }
        2
    }
}
