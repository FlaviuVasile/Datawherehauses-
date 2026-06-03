import requests

API_BASE_URL = "http://localhost:8080"


def print_section(title):
    print("\n" + "=" * 80)
    print(title)
    print("=" * 80)


def get(path, params=None):
    response = requests.get(f"{API_BASE_URL}{path}", params=params, timeout=20)
    response.raise_for_status()
    return response.json()


def post(path, params=None):
    response = requests.post(f"{API_BASE_URL}{path}", params=params, timeout=60)
    print("STATUS:", response.status_code)
    print("RESPONSE:", response.text)
    response.raise_for_status()
    return response.json()


print_section("1. Ingest Microsoft from Stooq")
print(post("/api/ingest/stooq", {
    "symbol": "msft.us",
    "assetId": "MSFT"
}))

print_section("2. Ingest Apple from Stooq")
print(post("/api/ingest/stooq", {
    "symbol": "aapl.us",
    "assetId": "AAPL"
}))

print_section("3. List assets")
print(get("/api/assets"))

print_section("4. List data sources")
print(get("/api/providers"))

print_section("5. Summarize MSFT trends")
print(get("/api/analytics/summary", {
    "assetId": "MSFT",
    "providerId": "STOOQ"
}))

print_section("6. Forecast MSFT next day")
print(get("/api/analytics/forecast/next-day", {
    "assetId": "MSFT",
    "providerId": "STOOQ"
}))

print_section("7. Compare AAPL and MSFT")
print(get("/api/analytics/compare", {
    "assetId1": "AAPL",
    "assetId2": "MSFT",
    "providerId": "STOOQ"
}))