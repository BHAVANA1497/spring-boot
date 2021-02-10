package com.hrs.enquiry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.enquiry.model.GuestDetails;
import com.hrs.enquiry.repository.GuestDetailsRepository;
import com.hrs.enquiry.rest.exception.EnquiryException;
import com.hrs.enquiry.rest.json.ParcelDetails;
import com.hrs.enquiry.service.EnquiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.MessageFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@Transactional
public class EnquiryServiceimpl implements EnquiryService {

    private ZoneId zoneId = ZoneId.systemDefault();

    private final GuestDetailsRepository guestDetailsRepository;

    @Autowired
    public EnquiryServiceimpl(GuestDetailsRepository guestDetailsRepository) {
        this.guestDetailsRepository = guestDetailsRepository;
    }
    /* *
    * Inserts Guest Details into the Database
    *
    * This method checks if Guest already present in the room
    *
    * If there is no Guest it will save the Record and return GuestDetails object
    * */
    @Override
    public GuestDetails insert(GuestDetails guestDetails){

      log.info("create new Guest record with info {}", guestDetails);

      Optional<GuestDetails> existingRecord = guestDetailsRepository.findByRoomNoAndStatus(guestDetails.getRoomNo(), "CHECKED_IN");

      if(existingRecord.isPresent()){
          log.error("Record Exist with Room No: {}, Guest Name: {}", guestDetails.getRoomNo(), guestDetails.getGuestName());
          throw new EnquiryException(MessageFormat.format(
                  "Guest already present in Room No: {0}", guestDetails.getRoomNo() ));
      }

        guestDetails.setStatus("CHECKED_IN");
        guestDetails.setCheckInTime(LocalDateTime.now(Clock.system(zoneId)));

      GuestDetails createdGuestRec = guestDetailsRepository.save(guestDetails);

        log.info("create new Guest record with info {}", createdGuestRec);

        return createdGuestRec;

    }

    /* *
     * Find All Guests with status CHECKED_IN
     *
     * This method retrives the details of guests
     * who are currently present in the Database and not checked out
     *
     * This Method allows receptionist to decide whether to accept the parcel or not
     * */
    @Override
    public List<GuestDetails> findAll() {
        return guestDetailsRepository.findByStatus("CHECKED_IN");
    }


    /* *
     * Update Guest Record with parcel Details
     *
     * This method allows to update parcel Details for existing Guest
     *
     * This Method accepts parcel Details and RoomNo, to check if the record is present to update and
     * to update the record with parcel Details
     *
     * This Method returns Guest Details after updating exsisting Guest Record
     * */
    @Override
    public GuestDetails updateGuestWithParcelDetails(List<ParcelDetails> parcelReq, String roomNo){

        log.info("update Guest record with parcel info {}, room no:{}", parcelReq, roomNo);

        Optional<GuestDetails> existingRecord = guestDetailsRepository.findByRoomNoAndStatus(roomNo, "CHECKED_IN");


        if(!existingRecord.isPresent()){
            log.error("No Guest Record with Room No: {} and status: {}", roomNo, "CHECKED_IN");
            throw new EnquiryException(MessageFormat.format(
                    "No Guest Record with Room No: {0} and status: {1}", roomNo, "CHECKED_IN" ));
        }

        GuestDetails updateRec = existingRecord.get();

        String parcelDetailsJson = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            parcelDetailsJson = mapper.writeValueAsString(parcelReq);
            log.info("parcel Details json:{}", parcelDetailsJson);
        } catch (Exception e) {
            log.info("unhandled excep {}", e.getCause());
        }

        updateRec.setParcelDetailsJson(parcelDetailsJson);

        GuestDetails updatedGuestRec = guestDetailsRepository.save(updateRec);

        log.info("updated Guest record with info {}", updatedGuestRec);

        return updatedGuestRec;

    }


    /* *
     * update Guest record while checkout
     *
     * This method retrieves Guest Details while guest checkout
     * which allows receptionist to check for parcels available for pick-up when guest is checking out
     *
     * This Method accepts roomNo as input to check if guest is present to checkout
     *This Method returns GuestDetails
     * */
    @Override
    public GuestDetails updateWhileCheckout( String roomNo){

        log.info("update Guest record while Checkout room no:{}", roomNo);

        Optional<GuestDetails> existingRecord = guestDetailsRepository.findByRoomNoAndStatus(roomNo, "CHECKED_IN");

        if(!existingRecord.isPresent()){
            log.error("No Guest Record with Room No: {} and status: {}", roomNo, "CHECKED_IN");
            throw new EnquiryException(MessageFormat.format(
                    "No Guest Record with Room No: {0} and status: {1}", roomNo, "CHECKED_IN" ));
        }

        GuestDetails updateRec = existingRecord.get();

        updateRec.setStatus("CHECKED_OUT");
        updateRec.setCheckOutTime(LocalDateTime.now(Clock.system(zoneId)));

        GuestDetails updatedGuestRec = guestDetailsRepository.save(updateRec);

        log.info("updated Guest record with info {}", updatedGuestRec);

        return updatedGuestRec;

    }




}
