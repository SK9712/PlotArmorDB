# PlotArmorDB
PlotArmorDB is a high-performance, Java-based vector database designed for AI-powered semantic search, recommendation systems, and LLM-based retrieval. It provides fast, scalable, and resilient vector search optimized for real-time AI applications.

## Why "PlotArmorDB"?
Like characters with plot armor in stories, your data always survives, adapts, and delivers fast, reliable results—no matter the challenge!

## Quick Start

```bash
# Build Docker image
docker build -t plotarmordb .

# Run container
docker run -d \
  -p 8080:8080 \
  -v plotarmor-data:/data/plotarmor-data \
  --name plotarmordb \
  plotarmordb

# Stop container
docker stop plotarmordb

# Remove container 
docker rm plotarmordb

# View logs
docker logs plotarmordb

# View container status
docker ps -a | grep plotarmordb

# Shell access
docker exec -it plotarmordb /bin/sh

# View container metrics
docker stats plotarmordb
```

Access the API:
- REST API: http://localhost:8080/vectors
- Swagger UI: http://localhost:8080/swagger-ui.html 
- Health check: http://localhost:8080/actuator/health

## Key Features

- Vector storage with RocksDB backend
- Fast nearest neighbor search using virtual threads
- Text-to-vector conversion
- Hybrid search (vector + keyword filtering)
- Result caching
- Docker support with health monitoring

## API Examples

Store vector:
```bash
curl -X POST http://localhost:8080/vectors \
  -H "Content-Type: application/json" \
  -d '{
    "values": [0.1, 0.2, 0.3],
    "metadata": {"category": "tech"}
  }'
```

Search vectors:
```bash
curl -X POST http://localhost:8080/vectors/search \
  -H "Content-Type: application/json" \
  -d '{
    "queryVector": [0.1, 0.2, 0.3],
    "topK": 5,
    "filter": {"category": "tech"}
  }'
```

Create from text:
```bash
curl -X POST http://localhost:8080/vectors/text \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Example document text",
    "metadata": {"category": "docs"}
  }'
```

## Configuration

Environment variables:
- `DB_PATH`: Database storage location
- `SERVER_PORT`: HTTP port (default: 8080)  
- `MAX_CACHE_SIZE`: Result cache size (default: 1000)
- `VOCABULARY_SIZE`: Embedding vocab size (default: 10000)
- `BATCH_SIZE`: Search batch size (default: 1000)

## Requirements

- Docker
- Java 21 (for development)
- Maven (for development)

## Development

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run locally
./mvnw spring-boot:run
```

## License

Apache License 2.0

Copyright 2024 PlotArmorDB

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
