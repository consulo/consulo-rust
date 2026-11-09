fn unused_local() {
    let /*unused_variables*/x = 1;
}

fn unused_param(/*unused_variables*/a: i32) {}

fn used(a: i32) -> i32 {
    a
}

fn underscore(_a: i32) {
    let _b = 1;
}

fn unused_in_tuple(p: (i32, i32)) -> i32 {
    let (a, /*unused_variables*/b) = p;
    a
}

fn assigned_never_read() {
    let /*missing:unused_variables*/mut x = 1;
    /*missing:unused_assignments*/x = 2;
}

fn overwritten_before_read() -> i32 {
    let mut x = /*missing:unused_assignments*/1;
    x = 2;
    x
}

fn read_in_loop() {
    let mut i = 0;
    while i < 10 {
        i += 1;
    }
}

fn shadowed() -> i32 {
    let /*unused_variables*/x = 1;
    let x = 2;
    x
}

fn used_in_closure() -> i32 {
    let a = 1;
    let f = || a;
    f()
}

fn if_let_binding(o: Option<i32>) {
    if let Some(/*unused_variables*/v) = o {}
}

fn match_binding(o: Option<i32>) -> i32 {
    match o {
        Some(/*unused_variables*/v) => 1,
        None => 0,
    }
}

fn for_binding() {
    for /*unused_variables*/i in [1, 2] {}
}

fn dead_store_in_branch(c: bool) -> i32 {
    let mut x = /*missing:unused_assignments*/0;
    if c {
        /*missing:unused_assignments*/x = 1;
    }
    x = 2;
    x
}

fn compound_never_read() {
    let /*missing:unused_variables*/mut x = 1;
    /*missing:unused_assignments*/x += 1;
}

struct S { a: i32 }

fn struct_pattern(s: S) {
    let S { /*unused_variables*/a } = s;
}

fn uninit_assigned_later() -> i32 {
    let x;
    x = 1;
    x
}
