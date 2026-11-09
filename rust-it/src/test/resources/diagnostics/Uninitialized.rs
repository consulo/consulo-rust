fn never_assigned() -> i32 {
    let x: i32;
    /*E0381*/x
}

fn assigned_in_one_branch(c: bool) -> i32 {
    let x: i32;
    if c {
        x = 1;
    }
    /*E0381*/x
}

fn assigned_in_both_branches(c: bool) -> i32 {
    let x: i32;
    if c {
        x = 1;
    } else {
        x = 2;
    }
    x
}

fn assigned_in_loop() -> i32 {
    let x: i32;
    loop {
        x = 1;
        break;
    }
    x
}

fn assigned_in_while(c: bool) -> i32 {
    let x: i32;
    while c {
        x = 1;
        break;
    }
    /*E0381*/x
}

fn diverging_branch(c: bool) -> i32 {
    let x: i32;
    if c {
        x = 1;
    } else {
        return 0;
    }
    x
}

fn match_arms(n: i32) -> i32 {
    let x: i32;
    match n {
        0 => x = 1,
        _ => {}
    }
    /*E0381*/x
}

fn let_else(o: Option<i32>) -> i32 {
    let x: i32;
    let Some(v) = o else {
        return /*E0381*/x;
    };
    v
}
