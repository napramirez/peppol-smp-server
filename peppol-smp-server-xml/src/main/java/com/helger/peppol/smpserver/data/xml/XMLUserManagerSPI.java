/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.data.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.data.IDataUser;
import com.helger.peppol.smpserver.data.ISMPUserManagerSPI;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.user.IUser;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * The DAO based {@link ISMPUserManagerSPI}.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@IsSPIImplementation
public final class XMLUserManagerSPI implements ISMPUserManagerSPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLUserManagerSPI.class);

  @Deprecated
  @UsedViaReflection
  public XMLUserManagerSPI ()
  {}

  public void createUser (final String sUserName, final String sPassword)
  {
    // not needed
  }

  public void deleteUser (final String sUserName)
  {
    // not needed
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <XMLDataUser> getAllUsers ()
  {
    final List <XMLDataUser> ret = new ArrayList <> ();
    for (final IUser aUser : AccessManager.getInstance ().getAllActiveUsers ())
      ret.add (new XMLDataUser (aUser));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public XMLDataUser getUserOfID (@Nullable final String sUserID)
  {
    final IUser aUser = AccessManager.getInstance ().getUserOfID (sUserID);
    return aUser == null ? null : new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPUnauthorizedException,
                                                                                                      SMPUnknownUserException
  {
    final AccessManager aAccessMgr = AccessManager.getInstance ();
    final IUser aUser = aAccessMgr.getUserOfLoginName (aCredentials.getUserName ());
    if (aUser == null)
    {
      s_aLogger.info ("Invalid login name provided: '" + aCredentials.getUserName () + "'");
      throw new SMPUnknownUserException (aCredentials.getUserName ());
    }
    if (!aAccessMgr.areUserIDAndPasswordValid (aUser.getID (), aCredentials.getPassword ()))
    {
      s_aLogger.info ("Invalid password provided for '" + aCredentials.getUserName () + "'");
      throw new SMPUnauthorizedException ("Username and/or password are invalid!");
    }
    return new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser createPreAuthenticatedUser (@Nonnull @Nonempty final String sUserName)
  {
    return new XMLDataUser (AccessManager.getInstance ().getUserOfLoginName (sUserName));
  }

  @Nonnull
  public ISMPServiceGroup verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                           @Nonnull final IDataUser aCurrentUser) throws SMPNotFoundException,
                                                                                  SMPUnauthorizedException
  {
    // Resolve user group
    final ISMPServiceGroup aServiceGroup = MetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " +
                                      IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                                      " does not exist");
    }

    // Resolve user
    final String sOwnerID = aServiceGroup.getOwnerID ();
    if (!sOwnerID.equals (aCurrentUser.getID ()))
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCurrentUser.getUserName () +
                                          "' does not own " +
                                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Verified service group " +
                       aServiceGroup.getID () +
                       " is owned by user '" +
                       aCurrentUser.getUserName () +
                       "'");

    return aServiceGroup;
  }
}