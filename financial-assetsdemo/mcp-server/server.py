from typing import Any, Dict, List, Optional

import requests
from mcp.server.fastmcp import FastMCP


API_BASE_URL = "http://localhost:8080"

mcp = FastMCP("Financial Assets Assistant")


def get_json(path: str, params: Optional[Dict[str, Any]] = None) -> Any:
    url = f"{API_BASE_URL}{path}"
    response = requests.get(url, params=params, timeout=20)
    response.raise_for_status()
    return response.json()


def post_json(path: str, params: Optional[Dict[str, Any]] = None) -> Any:
    url = f"{API_BASE_URL}{path}"
    response = requests.post(url, params=params, timeout=60)
    response.raise_for_status()
    return response.json()


@mcp.tool()
def list_assets() -> List[Dict[str, Any]]:
    """
    List all current financial assets available in the platform.
    """
    return get_json("/api/assets")


@mcp.tool()
def get_asset_details(asset_id: str) -> Dict[str, Any]:
    """
    Get full details for a specific financial asset by asset id.
    Example: asset_id='MSFT'
    """
    return get_json(f"/api/assets/{asset_id}")


@mcp.tool()
def list_data_sources() -> List[Dict[str, Any]]:
    """
    List all current financial data providers available in the platform.
    """
    return get_json("/api/providers")


@mcp.tool()
def get_data_source_details(provider_id: str) -> Dict[str, Any]:
    """
    Get full details for a financial data source by provider id.
    Example: provider_id='STOOQ'
    """
    return get_json(f"/api/providers/{provider_id}")


@mcp.tool()
def fetch_time_series(
    asset_id: str,
    provider_id: str,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None
) -> List[Dict[str, Any]]:
    """
    Fetch time-series market data for a given asset and provider.

    Dates are optional and must use YYYY-MM-DD format.
    Example:
    asset_id='MSFT', provider_id='STOOQ',
    start_date='2024-01-01', end_date='2024-12-31'
    """
    params = {
        "assetId": asset_id,
        "providerId": provider_id
    }

    if start_date and end_date:
        params["startDate"] = start_date
        params["endDate"] = end_date

    return get_json("/api/market-data/search", params=params)


@mcp.tool()
def summarize_trends(
    asset_id: str,
    provider_id: str,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None
) -> Dict[str, Any]:
    """
    Summarize trend, min, max, average and percentage change for an asset.
    """
    params = {
        "assetId": asset_id,
        "providerId": provider_id
    }

    if start_date and end_date:
        params["startDate"] = start_date
        params["endDate"] = end_date

    return get_json("/api/analytics/summary", params=params)


@mcp.tool()
def forecast_next_day(asset_id: str, provider_id: str) -> Dict[str, Any]:
    """
    Forecast the next-day close price using the platform's simple moving average model.
    """
    params = {
        "assetId": asset_id,
        "providerId": provider_id
    }

    return get_json("/api/analytics/forecast/next-day", params=params)


@mcp.tool()
def compare_assets(asset_id_1: str, asset_id_2: str, provider_id: str) -> Dict[str, Any]:
    """
    Compare two assets using the platform analytics endpoint.
    Example: asset_id_1='AAPL', asset_id_2='MSFT', provider_id='STOOQ'
    """
    params = {
        "assetId1": asset_id_1,
        "assetId2": asset_id_2,
        "providerId": provider_id
    }

    return get_json("/api/analytics/compare", params=params)


@mcp.tool()
def ingest_stooq(symbol: str, asset_id: str) -> Dict[str, Any]:
    """
    Ingest historical daily market data from Stooq.

    Examples:
    symbol='msft.us', asset_id='MSFT'
    symbol='aapl.us', asset_id='AAPL'
    """
    params = {
        "symbol": symbol,
        "assetId": asset_id
    }

    return post_json("/api/ingest/stooq", params=params)


@mcp.tool()
def explain_change(asset_id: str, provider_id: str) -> Dict[str, Any]:
    """
    Explain the latest observed change using grounded data from the platform.

    This tool does not use generic financial knowledge. It fetches the platform's
    stored time series and returns the latest close-to-close change.
    """
    data = fetch_time_series(asset_id=asset_id, provider_id=provider_id)

    valid_rows = [
        row for row in data
        if row.get("closePrice") is not None and row.get("dataDate") is not None
    ]

    if len(valid_rows) < 2:
        return {
            "assetId": asset_id,
            "providerId": provider_id,
            "message": "Not enough stored data to explain a change."
        }

    valid_rows = sorted(valid_rows, key=lambda row: row["dataDate"])

    previous = valid_rows[-2]
    latest = valid_rows[-1]

    previous_close = float(previous["closePrice"])
    latest_close = float(latest["closePrice"])

    absolute_change = latest_close - previous_close
    percentage_change = 0 if previous_close == 0 else (absolute_change / previous_close) * 100

    if absolute_change > 0:
        direction = "UP"
    elif absolute_change < 0:
        direction = "DOWN"
    else:
        direction = "FLAT"

    return {
        "assetId": asset_id,
        "providerId": provider_id,
        "previousDate": previous["dataDate"],
        "latestDate": latest["dataDate"],
        "previousClose": previous_close,
        "latestClose": latest_close,
        "absoluteChange": absolute_change,
        "percentageChange": percentage_change,
        "direction": direction,
        "grounding": "Computed only from market data returned by the platform API."
    }


if __name__ == "__main__":
    mcp.run()