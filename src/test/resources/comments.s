addq %rax, %rax		# comment1
addq %rax, %rax	# comment2
addq %rax, %rax	 #comment3
#addq %rax, %rax
addq $0x1122334455, %rax	# comment5
addq %rax, (%rax) # comment6
addq %rax, (%rax, %rbx) # comment7
addq %rax, (%rax, %rbx, 2) # comment8
addq %rax, 16(%rax, %rbx, 2) # comment9
addq %rax, %rax		# long long long comment10
# addq %rax, %rax
