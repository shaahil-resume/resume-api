Run the live evaluation with:

`OPENAI_API_KEY=... mvn -P eval verify -DskipTests=false`

To promote a reviewed candidate baseline:

`cp target/eval/baseline-candidate.json src/test/resources/eval/baseline.json`
