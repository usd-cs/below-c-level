addq
add $5, %rax
assq $5, %rax
addz $5, %rax
addq $5
addq $5, %rax, %rcx
addq $5, foo
addq $5, %rex
addq %rax, $5
addq $5 %rax
addq $5, %eax
addq 5, %rax
addq $4(%rsp), %rax
addq 4(%eax), %rax
addq 4(%rax, %ebx), %rax
addq 4(%rax, %rbx, 3), %rax
addq 4(%rax, %rbx, $2), %rax
addq 4(%rax, %rbx, %rcx), %rax
addq 4(1, 1, 1), %rax
addq %ebx, %rax
addb $1000, %al
pushl $5
pushq $5, %rax
popq $5
pushq
popq
movzql %rax, %ebx
movsql %rax, %ebx
shlq %rax, %rbx
leaq %rax, %rbx
leaq $11, %rbx
incq %rax, %rbx
sete %rbx
sete %bl, %al, %cl
sete %bl, %al
sete $9
sete
seteq %bl
je $5
je %rax
je (%rsp)
je
jeq bar
jmp
jmpq
retq $5
retq %rax
retq 8(%rax)
mylabel
mylabel: addq $5, %rax
pushq $5 -
pushq $5, 
addq (%rsp), 8(%rsp)