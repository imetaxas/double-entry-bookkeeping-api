Rules followed: paste EXACTLY what you typed, and the tool's raw answer location. Do not edit afterwards.
Tool + version: Claude 3.5 Sonnet   Date: 2026-10-26   Operator: Student

Paste-ready inputs: docs/ai-test-engineer/prompt-inputs/P1..P4 (template + source). Use a FRESH chat with no project context so the baseline is not influenced by known weak spots.
Put all AI output in src/test/java/com/yanimetaxas/bookkeeping/ai/ (so PIT v1 can target ONLY it).

#	Target	Prompt (verbatim)	Output file	Compiled?	Passed?	Edits you made (must be zero for v1, else list)
P1	Money, Account, TransactionLeg	"You are a Java 8 / JUnit 4 test engineer. Write unit tests for the class below. Use JUnit 4.12 and Mockito 1.x only. Cover normal, edge and error paths. Put them in package com.yanimetaxas.bookkeeping.ai. "	MoneyAiTest.java	Yes	Yes	0
P2	TransferRequest builder	"You are a Java 8 / JUnit 4 test engineer. Write unit tests for the class below. Use JUnit 4.12 and Mockito 1.x only. Cover normal, edge and error paths. Put them in package com.yanimetaxas.bookkeeping.ai. include the debit/credit sign behaviour [paste source]"	TransferRequestAiTest.java	Yes	Yes	Deleted 3 uncompilable test method (builderShouldReplaceAccountWhenAccountIsCalledAgain)
P3	TransferValidatorImpl	"You are a Java 8 / JUnit 4 test engineer. Write unit tests for the class below. Use JUnit 4.12 and Mockito 1.x only. Cover normal, edge and error paths. Put them in package com.yanimetaxas.bookkeeping.ai. mock AccountDao and TransactionDao [paste source]"	TransferValidatorImplAiTest.java	Yes	Yes	Deleted 2 uncompilable test method
P4	TransferServiceImpl	"You are a Java 8 / JUnit 4 test engineer. Write unit tests for the class below. Use JUnit 4.12 and Mockito 1.x only. Cover normal, edge and error paths. Put them in package com.yanimetaxas.bookkeeping.ai. mock the validator and both DAOs [paste source]"	TransferServiceImplAiTest.java	Yes	Yes	0
P5	(optional) refinement	"Improve the tests above to raise branch coverage"	—	N/A	N/A	—
Rules for a fair v1: (a) do not add assertions by hand to the AI files, (b) delete any AI test that does not compile or fails because it is wrong, and RECORD each deletion here, (c) never feed the AI the PIT report before v1 is measured.
Declaration of AI Tool Use must list this tool and these activities.
#	EDITS MADE TO AI OUTPUTS
TransferRequestAiTest.builderShouldReplaceAccountWhenAccountIsCalledAgain
TransferRequestAiTest.toMoneyShouldRejectNullAmountString
TransferRequestAiTest.builderShouldNotEnforceMinimumLegCount
TransferValidatorImplAiTest.validateTransferRequestShouldRejectEmptyRequest
TransferValidatorImplAiTest.isTransactionBalancedShouldRejectDifferentBigDecimalScale

Record of the V1 numbers
|Setting	|Generated	|Killed	|Survived	|No coverage	Score|
|DEFAULTS mutators|	72	|69	|3	|0	|96%  |
|ALL mutators (headline v1)	|252	|240	|12	|0	|95.2%|
