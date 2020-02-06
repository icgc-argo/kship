TOPIC='source-topic'
compose:
	docker-compose -f docker-compose.yaml up -d
stop:
	docker-compose down --remove-orphans
nuke:
	docker-compose down --volumes --remove-orphans
k-topic:
	curl -X GET http://localhost:8082/topics/$(TOPIC) -H 'Accept: */*' | jq
k-msg:
	curl -X POST http://localhost:8082/topics/$(TOPIC) \
	-H 'Accept: application/vnd.kafka.v2+json' -H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-d '{"records": [{ "value": { "name": "$(MSG)"  }}]}' | jq