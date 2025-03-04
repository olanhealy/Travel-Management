import React, { useState, useEffect } from 'react';
import './HotelDiscovery.css';

const HotelDiscovery = () => {
    const [location, setLocation] = useState('');
    const [hotels, setHotels] = useState([]);
  
    // Fetch all hotels when the component mounts
    useEffect(() => {
      fetchHotels();
    }, []);

    const fetchHotels = (location = '') => {
      const url = location 
        ? `http://localhost:8080/api/hotels/search?location=${location}`
        : `http://localhost:8080/api/hotels/getAll`; // Get all hotels when no location is specified
      
      fetch(url)
        .then((response) => response.json())
        .then((data) => setHotels(data))
        .catch((error) => console.error('Error fetching hotels:', error));
    };

    const handleSearch = () => {
      if (!location) {
        fetchHotels(); // If no location is entered, show all hotels
      } else {
        fetchHotels(location); // Otherwise, search hotels by location
      }
    };

    return (
      <div className="hotel-discovery">
        {/* Search bar */}
        <div className="search-bar">
          <input
            type="text"
            placeholder="Search by location..."
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
          <button onClick={handleSearch}>Search</button>
        </div>
  
        {/* Hotel list */}
        <div className="hotel-list">
          {hotels.length === 0 ? (
            <p>No hotels found for the selected location.</p>
          ) : (
            hotels.map((hotel) => (
              <div className="hotel-card" key={hotel.id}>
                <h3>{hotel.name}</h3>
                <p>{hotel.location}</p>
                <p>${hotel.pricePerNight} per night</p>
                <button>View Details</button>
              </div>
            ))
          )}
        </div>
      </div>
    );
}

export default HotelDiscovery;