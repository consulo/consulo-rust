fn diverge() -> ! {
    loop {}
}

fn after_return() -> i32 {
    return 1;
    let x = 2;
    x
}

fn after_call() {
    diverge();
    let _ = 1;
}

fn after_break() {
    loop {
        break;
        let _ = 1;
    }
}

fn after_continue() {
    for _ in [1, 2] {
        continue;
        let _ = 1;
    }
}

fn return_in_branch(c: bool) -> i32 {
    if c {
        return 1;
    } else {
        return 2;
    }
    3
}

fn try_op(r: Result<i32, ()>) -> Result<i32, ()> {
    let v = r?;
    Ok(v)
}

fn after_infinite() {
    loop {}
    let _ = 1;
}
