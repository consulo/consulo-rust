struct NotCopy;

fn consume(_: NotCopy) {}

fn use_after_move() {
    let a = NotCopy;
    consume(a);
    consume(/*E0382*/a);
}

fn move_in_one_branch(c: bool) {
    let a = NotCopy;
    if c {
        consume(a);
    }
    consume(/*E0382*/a);
}

fn move_in_both_branches_reassigned(c: bool) {
    let mut a = NotCopy;
    if c {
        consume(a);
    } else {
        consume(a);
    }
    a = NotCopy;
    consume(a);
}

fn move_in_loop() {
    let a = NotCopy;
    loop {
        consume(/*E0382*/a);
    }
}

fn move_into_variable() {
    let a = NotCopy;
    let b = a;
    let _c = /*E0382*/a;
    let _d = b;
}

fn move_in_labeled_block() {
    let a = NotCopy;
    let b = 'l: {
        a
    };
    consume(b);
}

fn copy_is_fine() -> i32 {
    let a = 1;
    let b = a;
    a + b
}

fn move_into_closure() {
    let a = NotCopy;
    let f = move || consume(a);
    f();
    consume(/*E0382*/a);
}

fn move_out_of_tuple() {
    let t = (NotCopy, NotCopy);
    let (x, _) = t;
    consume(x);
    consume(/*missing:E0382*/t.0);
}

fn reinit_in_loop() {
    let mut a = NotCopy;
    for _ in [1, 2] {
        consume(a);
        a = NotCopy;
    }
}
