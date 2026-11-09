fn diverge() -> ! {
    loop {}
}

fn after_return() -> i32 {
    return 1;
    /*unreachable_code*/let x = 2;
    /*false:unreachable_code*/x
}

fn after_call() {
    diverge();
    /*unreachable_code*/let _ = 1;
}

fn after_break() {
    loop {
        break;
        /*unreachable_code*/let _ = 1;
    }
}

fn after_continue() {
    for _ in [1, 2] {
        continue;
        /*unreachable_code*/let _ = 1;
    }
}

fn both_branches(c: bool) -> i32 {
    if c {
        return 1;
    } else {
        return 2;
    }
    /*unreachable_code*/3
}

fn after_infinite() {
    loop {}
    /*unreachable_code*/let _ = 1;
}

fn while_true_is_not_infinite(mut n: i32) {
    while true {
        n += 1;
    }
    let _ = n;
}

fn reachable_after_loop_with_break() -> i32 {
    let mut i = 0;
    loop {
        i += 1;
        if i > 3 {
            break;
        }
    }
    i
}

fn labeled_block(c: bool) -> i32 {
    'b: {
        if c {
            break 'b 1;
        }
        2
    }
}

fn match_all_return(n: i32) -> i32 {
    match n {
        0 => return 0,
        _ => return 1,
    }
}

fn in_closure() -> i32 {
    let f = || {
        return 1;
        /*unreachable_code*/2
    };
    f()
}

fn return_in_argument() -> i32 {
    /*false:unreachable_code*/let _x = (return 1, /*missing:unreachable_code*/2);
}
